package com.scaffold.framework.satoken;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置
 * <p>
 * 路由鉴权策略：
 * - /admin/** 管理端/服务端接口：需要后台登录鉴权（StpUtil.checkLogin）
 * - /app/** 小程序端/后厨端接口：需要小程序登录鉴权（Sa-Token session，后续通过 openid 登录创建）
 * - /wx/pay/** 微信支付回调：公开访问，无需鉴权
 * - /ws/** WebSocket 端点：公开访问，无需鉴权
 * - 其他公共路径（登录、注册、接口文档等）：公开访问
 *
 * @author Henfon
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * 公开访问路径（无需任何鉴权）
     */
    private static final String[] PUBLIC_PATHS = {
            // 登录注册
            "/auth/login",
            "/auth/register",
            // 小程序手机号登录
            "/app/auth/phone-login",
            // 小程序公开浏览能力：菜单、桌台识别、开台
            "/app/dish/list",
            "/app/dish/search",
            "/app/dish/category/list",
            "/app/banner/list",
            "/app/table/*",
            "/app/table/*/open",
            // 路由（常量路由和路由检查不需要登录）
            "/route/getConstantRoutes",
            "/route/isRouteExist",
            // 接口文档
            "/doc.html",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/webjars/**",
            // 静态资源
            "/favicon.ico",
            "/error",
            // 支付回调（第三方平台异步通知，需公开访问）
            "/wx/pay/**",
            // WebSocket 端点
            "/ws/**"
    };

    /**
     * 注册 Sa-Token 拦截器
     *
     * @author Henfon
     * @date 2026-07-12
     * @description 使用统一的公开路径匹配逻辑处理鉴权，确保扫码桌台等匿名接口按预期放行。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            final String requestPath = SaHolder.getRequest().getRequestPath();

            // 公开接口直接放行，避免通配符路径在不同请求下出现误判。
            if (isPublicPath(requestPath)) {
                return;
            }

            // 其余接口沿用统一登录校验，保持管理端与小程序端现有行为不变。
            StpUtil.checkLogin();
        })).addPathPatterns("/**");
    }

    /**
     * 判断当前请求是否为公开路径
     *
     * @author Henfon
     * @date 2026-07-12
     * @description 使用 Ant 风格路径匹配公开接口，兼容桌台扫码等带路径变量的匿名访问场景。
     * @param requestPath 当前请求路径
     * @return true-公开访问；false-需要登录
     */
    private boolean isPublicPath(String requestPath) {
        if (requestPath == null || requestPath.isBlank()) {
            return false;
        }

        // 逐个匹配公开路径，保证 /app/table/*、/swagger-ui/** 等规则都能稳定命中。
        for (String publicPath : PUBLIC_PATHS) {
            if (PATH_MATCHER.match(publicPath, requestPath)) {
                return true;
            }
        }

        return false;
    }
}
