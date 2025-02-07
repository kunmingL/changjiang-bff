package com.changjing.bff.aspect;

// import com.changjing.bff.dto.LoginUserInfo;
// import com.changjing.bff.object.response.Result;
// import io.micrometer.common.util.StringUtils;
// import jakarta.servlet.http.HttpSession;
// import org.springframework.http.ResponseEntity;
// import org.springframework.stereotype.Component;
// import org.springframework.web.context.request.RequestContextHolder;
// import org.springframework.web.context.request.ServletRequestAttributes;

// import java.lang.reflect.Method;

//@Component
//@Aspect
//public class ConcurrentRequestInterceptAspect {
//
//    private static final String LOGIN_USER_INFO = "login_user_info";
//
//    @Pointcut("@annotation(com.citicbank.npcs.pcgtw.annotation.ConcurrentRequestIntercept)")
//    private void pointcut() {}
//
//    @Around("pointcut()")
//    public Object before(ProceedingJoinPoint joinPoint) throws Throwable {
//        if (RequestContextHolder.getRequestAttributes() == null) {
//            throw new RuntimeException("ConcurrentRequestInterceptAspect request attributes is null");
//        }
//        if (RequestContextHolder.getRequestAttributes().getRequest() == null) {
//            throw new RuntimeException("ConcurrentRequestInterceptAspect request getRequestAttributes is null");
//        }
//        if (((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest() == null) {
//            throw new RuntimeException("ConcurrentRequestInterceptAspect request request is null");
//        }
//
//        HttpSession httpSession = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getSession();
//        if (httpSession == null) {
//            throw new RuntimeException("ConcurrentRequestInterceptAspect request httpSession is null");
//        }
//
//        LoginUserInfo userInfo = (LoginUserInfo) httpSession.getAttribute(LOGIN_USER_INFO);
//        if (userInfo != null && userInfo.getUser() != null && StringUtils.isNotBlank(userInfo.getUser().getName())) {
//            String userName = userInfo.getUser().getName();
//            String uri = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getRequestURI();
//
//            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
//            ConcurrentRequestIntercept args = method.getAnnotation(ConcurrentRequestIntercept.class);
//            long intervalTime = args.expireMillisecond();
//
//            if (PreventRepeatUtil.isRepeat(httpSession, userName, uri, intervalTime)) {
//                Result handleResult = new Result();
//                handleResult.setCode("AAAAAAA");
//                handleResult.setMsg("请求重复");
//                return ResponseEntity.ok().header("clazz", handleResult.getClass().getName()).body(handleResult);
//            }
//        }
//        return joinPoint.proceed();
//    }
//}
