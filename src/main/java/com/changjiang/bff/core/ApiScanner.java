package com.changjiang.bff.core;

import com.changjiang.bff.annotation.ServiceConfig;
import com.changjiang.bff.config.ServiceScanProperties;
import com.changjiang.grpc.factory.GrpcServiceFactory;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API扫描器
 *
 * 该类的主要职责是动态加载外部JAR包，扫描其中的服务接口注解，并注册服务API信息。
 * 具体功能包括：
 * 1. **动态加载外部JAR包**：根据配置的Maven坐标，从远程仓库下载并加载JAR包。
 * 2. **扫描服务接口注解**：使用反射和`Reflections`库扫描JAR包中的类和方法，查找带有`@ServiceConfig`注解的方法。
 * 3. **注册服务API信息**：将扫描到的服务接口信息封装为`ServiceApiInfo`对象，并存储在内存中以供后续使用。
 */
@Component
public class ApiScanner {

    private static final Logger logger = LoggerFactory.getLogger(ApiScanner.class);

    // 用于存储扫描到的服务API信息，键为服务的唯一标识（URL或Registry ID），值为服务信息对象
    private final ConcurrentHashMap<String, ServiceApiInfo> apiRegistry = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, ServiceApiInfo> getApiRegistry() {
        return apiRegistry;
    }

    @Autowired
    private GrpcServiceFactory grpcServiceFactory;

    @Autowired
    private  ServiceScanProperties serviceScanProperties; // 从配置文件中读取扫描配置

    // public ApiScanner(ServiceScanProperties serviceScanProperties) {
    //     this.serviceScanProperties = serviceScanProperties;
    // }

    /**
     * 监听Spring应用启动完成事件，触发API扫描。
     * 当Spring应用启动完成后，会自动调用此方法。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Spring应用启动完成，开始执行API扫描...");
        try {
            scanAllApis(); // 执行扫描逻辑
        } catch (Exception e) {
            logger.error("API扫描失败", e);
        }
    }

    /**
     * 扫描所有配置的API。
     * 1. 获取配置的Maven坐标列表。
     * 2. 初始化Maven仓库系统。
     * 3. 解析并下载每个坐标对应的JAR文件。
     * 4. 扫描每个JAR文件，查找带有`@ServiceConfig`注解的方法。
     * 5. 打印扫描结果。
     */
    public void scanAllApis() {
        logger.info("开始扫描所有配置的API...");

        // 1. 获取配置的Maven坐标列表
        List<String> coordinates = serviceScanProperties.getCoordinates();
        if (coordinates == null || coordinates.isEmpty()) {
            logger.warn("未配置需要扫描的Maven坐标，跳过扫描");
            return;
        }
        logger.info("需要扫描的Maven坐标: {}", coordinates);

        // 2. 初始化Maven仓库系统
        RepositorySystem system = newRepositorySystem();
        RepositorySystemSession session = createRepositorySession(system);
        List<RemoteRepository> repositories = getRemoteRepositories();

        // 3. 处理每个坐标
        for (String coordinate : coordinates) {
            try {
                logger.info("开始处理坐标: {}", coordinate);

                // 4. 解析并下载依赖
                Set<File> jarFiles = resolveArtifact(system, session, repositories, coordinate);
                logger.info("坐标 {} 解析完成，下载的JAR文件: {}", coordinate, jarFiles);

                // 5. 扫描每个JAR文件
                for (File jarFile : jarFiles) {
                    logger.info("开始扫描JAR文件: {}", jarFile.getName());
                    scanJarFile(jarFile);
                }
            } catch (Exception e) {
                logger.error("处理坐标 {} 时发生错误", coordinate, e);
            }
        }

        // 6. 打印扫描结果
        printScanResults();
    }

    /**
     * 扫描单个JAR文件。
     * 1. 创建类加载器，加载JAR文件。
     * 2. 使用`Reflections`库扫描JAR文件中的类和方法，查找带有`@ServiceConfig`注解的方法。
     * 3. 处理每个找到的方法，将其注册为服务API。
     *
     * @param jarFile 需要扫描的JAR文件
     */
    private void scanJarFile(File jarFile) {
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader())) {
            logger.info("创建类加载器，加载JAR文件: {}", jarFile.getName());

            // 配置Reflections扫描器
            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .setUrls(classLoader.getURLs())
                    .addClassLoaders(classLoader)
                    .setScanners(Scanners.MethodsAnnotated));

            // 查找带有@ServiceConfig注解的方法
            Set<Method> methods = reflections.getMethodsAnnotatedWith(ServiceConfig.class);
            logger.info("在JAR文件 {} 中找到 {} 个带有@ServiceConfig注解的方法", jarFile.getName(), methods.size());

            // 处理每个找到的方法
            for (Method method : methods) {
                processMethod(method);
            }
        } catch (Exception e) {
            logger.error("扫描JAR文件 {} 时发生错误", jarFile.getName(), e);
        }
    }

    /**
     * 处理方法，将其注册为服务API。
     * 1. 获取方法上的`@ServiceConfig`注解。
     * 2. 创建`ServiceApiInfo`对象，封装方法和注解信息。
     * 3. 创建方法所属类的实例。
     * 4. 将服务API信息存入`apiRegistry`。
     *
     * @param method 需要处理的方法
     */
    private void processMethod(Method method) {
        logger.info("处理方法: {}.{}", method.getDeclaringClass().getName(), method.getName());

        // 1. 获取方法上的@ServiceConfig注解
        ServiceConfig configAnnotation = method.getAnnotation(ServiceConfig.class);
        if (configAnnotation == null) {
            logger.warn("方法 {}.{} 未找到@ServiceConfig注解，跳过处理", method.getDeclaringClass().getName(), method.getName());
            return;
        }

        // 2. 创建ServiceApiInfo对象
        if (configAnnotation == null) {
            logger.warn("Method {}.{} has no @ServiceConfig annotation",
                    method.getDeclaringClass().getName(), method.getName());
            return;
        }

        // 2. 创建gRPC客户端实例
       // Object grpcClient = createGrpcClient(method.getDeclaringClass());
        Object grpcClient = grpcServiceFactory.createServiceFromLoadedClass(configAnnotation.registryId(),method.getDeclaringClass());

        // 3. 构建ServiceApiInfo
        ServiceApiInfo apiInfo = ServiceApiInfo.builder()
                .method(method)
                .serviceConfig(configAnnotation)
                .instance(grpcClient)
                // 获取方法参数类型和返回类型
                .requestType(method.getParameterTypes().length > 0 ?
                        method.getParameterTypes() : null)
                .responseType(method.getReturnType())
                .methodName(method.getName())
                // HTTP相关信息
                .url(configAnnotation.url())
                .registryId(configAnnotation.registryId())
                .build();


        // 4. 将服务API信息存入apiRegistry
        String key = configAnnotation.url();
        apiRegistry.put(key, apiInfo);
        logger.info("成功注册服务API: {}.{}，键: {}", method.getDeclaringClass().getName(), method.getName(), key);
    }

    /**
     * 打印扫描结果。
     * 遍历`apiRegistry`，打印所有已注册的服务API信息。
     */
    private void printScanResults() {
        logger.info("=== API扫描结果 ===");
        logger.info("共找到 {} 个服务API", apiRegistry.size());

        apiRegistry.forEach((key, apiInfo) -> {
            Method method = apiInfo.getMethod();
            ServiceConfig config = apiInfo.getServiceConfig();
            logger.info("API: {}.{}", method.getDeclaringClass().getName(), method.getName());
            logger.info("  - URL: {}", config.url());
            logger.info("  - Registry ID: {}", config.registryId());
            logger.info("------------------------");
        });
    }

    /**
     * 创建Maven仓库系统。
     * 使用Aether库初始化Maven仓库系统，用于解析和下载依赖。
     *
     * @return 初始化后的RepositorySystem对象
     */
    private RepositorySystem newRepositorySystem() {
        logger.info("初始化Maven仓库系统...");
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    /**
     * 创建Maven仓库会话。
     * 配置本地仓库路径和其他会话参数。
     *
     * @param system 仓库系统
     * @return 初始化后的RepositorySystemSession对象
     */
    private RepositorySystemSession createRepositorySession(RepositorySystem system) {
        logger.info("创建Maven仓库会话，本地仓库路径: {}", serviceScanProperties.getLocalRepository());
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(serviceScanProperties.getLocalRepository());
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        return session;
    }

    /**
     * 获取远程仓库列表。
     * 默认返回Maven中央仓库和阿里云Maven仓库。
     *
     * @return 远程仓库列表
     */
    private List<RemoteRepository> getRemoteRepositories() {
        logger.info("获取远程仓库列表...");
        return Arrays.asList(
                new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build(),
                new RemoteRepository.Builder("aliyun", "default", "https://maven.aliyun.com/repository/public").build()
        );
    }

    /**
     * 解析并下载制品。
     * 根据Maven坐标从远程仓库或本地仓库下载JAR文件。
     *
     * @param system       仓库系统
     * @param session      仓库会话
     * @param repositories 远程仓库列表
     * @param coordinate   Maven坐标
     * @return 下载的JAR文件集合
     * @throws DependencyResolutionException 如果依赖解析失败
     */
    private Set<File> resolveArtifact(RepositorySystem system, RepositorySystemSession session,
                                      List<RemoteRepository> repositories, String coordinate)
            throws DependencyResolutionException {
        logger.info("解析Maven坐标: {}", coordinate);

        String[] split = coordinate.split(":");
        String artifactId = split[0];
        String version = split[1];
        String groupId = "com.changjiang"; // 你可以根据需要修改默认的 groupId
        Artifact artifact = new DefaultArtifact(groupId, artifactId, "jar", version);
        // 2. 优先从远程仓库查找
        // logger.info("Trying to resolve artifact from remote repositories: {}:{}:{}", groupId, artifactId, version);
        // CollectRequest collectRequest = new CollectRequest(new Dependency(artifact, ""), repositories);
        // DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);
        //
        // try {
        //     // 3. 尝试从远程仓库解析依赖
        //     DependencyResult dependencyResult = system.resolveDependencies(session, dependencyRequest);
        //     Set<File> jarFiles = new HashSet<>();
        //     for (ArtifactResult artifactResult : dependencyResult.getArtifactResults()) {
        //         File artifactFile = artifactResult.getArtifact().getFile();
        //         jarFiles.add(artifactFile);
        //         logger.info("Artifact resolved from remote repository: {}", artifactFile.getAbsolutePath());
        //     }
        //     return jarFiles;
        // } catch (DependencyResolutionException e) {
            logger.warn("Failed to resolve artifact from remote repositories: {}:{}:{}", groupId, artifactId, version);

            // 4. 如果远程仓库未找到，回退到本地仓库
            File localFile = getLocalArtifactFile(session.getLocalRepository().getBasedir(), artifact);
            if (localFile.exists()) {
                logger.info("Artifact found in local repository: {}", localFile.getAbsolutePath());
                return Collections.singleton(localFile);
            } else {
                logger.error("Artifact not found in local repository: {}:{}:{}", groupId, artifactId, version);
                return null;
                //throw e; // 如果本地仓库也没有，抛出异常
            }
        //}
    }

    /**
     * 获取本地仓库中对应 Artifact 的文件路径。
     */
    private File getLocalArtifactFile(File localRepoPath, Artifact artifact) {
        String path = String.format("%s/%s/%s/%s/%s-%s.%s",
                localRepoPath,
                artifact.getGroupId().replace('.', '/'),
                artifact.getArtifactId(),
                artifact.getVersion(),
                artifact.getArtifactId(),
                artifact.getVersion(),
                artifact.getExtension());
        return new File(path);
    }
}