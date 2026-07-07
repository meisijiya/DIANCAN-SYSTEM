package com.scaffold.framework.satoken;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
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
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 1. /admin/** 管理端路径：后台登录鉴权
            SaRouter.match("/admin/**")
                    .notMatch(PUBLIC_PATHS)
                    .check(r -> StpUtil.checkLogin());

            // 2. /app/** 小程序端路径：小程序登录鉴权
            //    当前阶段使用 StpUtil.checkLogin()，后续小程序登录模块会通过 openid 创建 Sa-Token session
            SaRouter.match("/app/**")
                    .notMatch(PUBLIC_PATHS)
                    .check(r -> StpUtil.checkLogin());

            // 3. 其他未匹配 /admin/ 或 /app/ 的路径：仍需登录校验
            SaRouter.match("/**")
                    .notMatch(PUBLIC_PATHS)
                    .notMatch("/admin/**")
                    .notMatch("/app/**")
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}
