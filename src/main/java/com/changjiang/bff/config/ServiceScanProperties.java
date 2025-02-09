package com.changjiang.bff.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务扫描配置类
 *
 * 该类用于从配置文件中读取服务扫描的相关配置，并提供给 {@link com.changjiang.bff.core.ApiScanner} 使用。
 * 通过 Spring Boot 的 {@link ConfigurationProperties} 注解，可以将配置文件中的属性绑定到该类的字段中。
 *
 * ### 功能
 * 1. **提供 Maven 坐标列表**：用于指定需要扫描的外部 JAR 包的 Maven 坐标。
 * 2. **提供本地 Maven 仓库路径**：用于指定本地 Maven 仓库的路径，默认值为 `~/.m2/repository`。
 *
 * ### 配置文件示例
 * 在 `application.yml` 或 `application.properties` 中配置如下：
 *
 * #### YAML 格式
 * ```yaml
 * service:
 *   scan:
 *     coordinates: my-library:1.0.0;another-library:2.0.0
 *     local-repository: /path/to/local/repository
 * ```
 *
 * #### Properties 格式
 * ```properties
 * service.scan.coordinates=my-library:1.0.0;another-library:2.0.0
 * service.scan.local-repository=/path/to/local/repository
 * ```
 *
 * ### 字段说明
 * - `coordinates`：Maven 坐标列表，格式为 `artifactId:version`，多个坐标以分号（`;`）分割。
 * - `localRepository`：本地 Maven 仓库路径，默认值为 `~/.m2/repository`。
 */
@Data
@Component
@ConfigurationProperties(prefix = "service.scan")
public class ServiceScanProperties {
    /**
     * Maven 坐标列表
     * 格式: artifactId:version，多个坐标以分号（`;`）分割。
     * 示例: my-library:1.0.0;another-library:2.0.0
     */
    private List<String> coordinates;

    /**
     * 本地 Maven 仓库路径
     * 默认值为用户主目录下的 `.m2/repository`，即 `~/.m2/repository`。
     * 可以通过配置文件覆盖该值。
     */
    private String localRepository = System.getProperty("user.home") + "/.m2/repository";

    /**
     * 设置 Maven 坐标列表
     * 该方法会将传入的字符串按分号（`;`）分割，并转换为 List<String> 类型。
     *
     * @param coordinates 以分号分割的 Maven 坐标字符串
     */
    public void setCoordinates(String coordinates) {
        if (coordinates != null && !coordinates.isEmpty()) {
            this.coordinates = Arrays.stream(coordinates.split(";"))
                    .map(String::trim) // 去除空格
                    .filter(s -> !s.isEmpty()) // 过滤空字符串
                    .collect(Collectors.toList());
        } else {
            this.coordinates = List.of(); // 如果为空，设置为空列表
        }
    }
}