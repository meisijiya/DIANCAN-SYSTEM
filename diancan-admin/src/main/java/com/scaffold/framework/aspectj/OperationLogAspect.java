package com.scaffold.framework.aspectj;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.scaffold.modules.system.entity.SysOperationLog;
import com.scaffold.modules.system.entity.SysUser;
import com.scaffold.modules.system.mapper.SysUserMapper;
import com.scaffold.modules.system.service.SysOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面
 *
 * @author Henfon
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysOperationLogService operationLogService;
    private final SysUserMapper userMapper;

    @Around("within(com.scaffold.modules..controller..*) && " +
            "(@annotation(com.scaffold.framework.aspectj.OperationLog) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();
        String requestUrl = request == null ? "" : request.getRequestURI();

        // Skip sensitive/noisy endpoints.
        if (shouldSkip(requestUrl)) {
            return point.proceed();
        }

        long startTime = System.currentTimeMillis();
        SysOperationLog logEntity = new SysOperationLog();
        logEntity.setCreateTime(LocalDateTime.now());
        logEntity.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());
        logEntity.setModule(resolveModule(signature, method));
        logEntity.setOperation(resolveOperation(method));

        if (request != null) {
            logEntity.setRequestUrl(requestUrl);
            logEntity.setRequestMethod(request.getMethod());
            logEntity.setIp(getIpAddress(request));
        }

        try {
            Object[] args = point.getArgs();
            if (args != null && args.length > 0) {
                String params = JSONUtil.toJsonStr(args);
                logEntity.setRequestParams(StrUtil.sub(params, 0, 2000));
            }
        } catch (Exception e) {
            log.warn("获取请求参数失败", e);
        }

        fillUserInfo(logEntity);

        try {
            Object result = point.proceed();
            logEntity.setStatus(1);
            try {
                String responseResult = JSONUtil.toJsonStr(result);
                logEntity.setResponseResult(StrUtil.sub(responseResult, 0, 2000));
            } catch (Exception e) {
                log.warn("序列化响应结果失败", e);
            }
            return result;
        } catch (Throwable e) {
            logEntity.setStatus(0);
            logEntity.setErrorMsg(StrUtil.sub(e.getMessage(), 0, 2000));
            throw e;
        } finally {
            logEntity.setDuration(System.currentTimeMillis() - startTime);
            operationLogService.recordOperationLog(logEntity);
        }
    }

    private void fillUserInfo(SysOperationLog logEntity) {
        try {
            if (!StpUtil.isLogin()) {
                return;
            }
            String loginId = String.valueOf(StpUtil.getLoginId());
            try {
                long userId = Long.parseLong(loginId);
                logEntity.setUserId(userId);
                SysUser user = userMapper.selectById(userId);
                logEntity.setUsername(user == null ? loginId : user.getUsername());
            } catch (NumberFormatException ignore) {
                // miniapp may use openid/string login id
                logEntity.setUsername(loginId);
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败", e);
        }
    }

    private String resolveModule(MethodSignature signature, Method method) {
        OperationLog operationLog = method.getAnnotation(OperationLog.class);
        if (operationLog != null && StrUtil.isNotBlank(operationLog.module())) {
            return operationLog.module();
        }

        java.lang.annotation.Annotation rawTag = signature.getDeclaringType().getAnnotation(Tag.class);
        if (rawTag instanceof Tag tag && StrUtil.isNotBlank(tag.name())) {
            return tag.name();
        }
        return "系统";
    }

    private String resolveOperation(Method method) {
        OperationLog operationLog = method.getAnnotation(OperationLog.class);
        if (operationLog != null && StrUtil.isNotBlank(operationLog.operation())) {
            return operationLog.operation();
        }

        Operation operation = method.getAnnotation(Operation.class);
        if (operation != null && StrUtil.isNotBlank(operation.summary())) {
            return operation.summary();
        }

        if (method.getAnnotation(PostMapping.class) != null) {
            return "POST操作";
        }
        if (method.getAnnotation(PutMapping.class) != null) {
            return "PUT操作";
        }
        if (method.getAnnotation(DeleteMapping.class) != null) {
            return "DELETE操作";
        }
        return method.getName();
    }

    private boolean shouldSkip(String url) {
        if (StrUtil.isBlank(url)) {
            return false;
        }
        return url.contains("/auth/login")
                || url.contains("/auth/register")
                || url.contains("/auth/logout")
                || url.contains("/user/password")
                || url.contains("/payment/callback")
                || url.contains("/wx/pay/");
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
