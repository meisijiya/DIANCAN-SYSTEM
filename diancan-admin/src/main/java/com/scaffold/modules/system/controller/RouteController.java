package com.scaffold.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.scaffold.common.result.Result;
import com.scaffold.modules.system.service.SysMenuService;
import com.scaffold.modules.system.vo.RouteVO;
import com.scaffold.modules.system.vo.UserRouteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 路由控制器
 *
 * @author Henfon
 */
@Tag(name = "路由管理")
@RestController
@RequestMapping("/route")
@RequiredArgsConstructor
public class RouteController {

    private final SysMenuService menuService;

    @Operation(summary = "获取常量路由")
    @GetMapping("/getConstantRoutes")
    public Result<List<RouteVO>> getConstantRoutes() {
        List<RouteVO> routes = new ArrayList<>();
        
        // 登录页
        RouteVO login = new RouteVO();
        login.setName("login");
        login.setPath("/login/:module(pwd-login|code-login|register|reset-pwd|bind-wechat)?");
        login.setComponent("layout.blank$view.login");
        login.setId("login");
        RouteVO.RouteMeta loginMeta = new RouteVO.RouteMeta();
        loginMeta.setTitle("login");
        loginMeta.setI18nKey("route.login");
        loginMeta.setConstant(true);
        loginMeta.setHideInMenu(true);
        login.setMeta(loginMeta);
        routes.add(login);
        
        // 403
        RouteVO forbidden = new RouteVO();
        forbidden.setName("403");
        forbidden.setPath("/403");
        forbidden.setComponent("layout.blank$view.403");
        forbidden.setId("403");
        RouteVO.RouteMeta forbiddenMeta = new RouteVO.RouteMeta();
        forbiddenMeta.setTitle("403");
        forbiddenMeta.setI18nKey("route.403");
        forbiddenMeta.setConstant(true);
        forbiddenMeta.setHideInMenu(true);
        forbidden.setMeta(forbiddenMeta);
        routes.add(forbidden);
        
        // 404
        RouteVO notFound = new RouteVO();
        notFound.setName("404");
        notFound.setPath("/404");
        notFound.setComponent("layout.blank$view.404");
        notFound.setId("404");
        RouteVO.RouteMeta notFoundMeta = new RouteVO.RouteMeta();
        notFoundMeta.setTitle("404");
        notFoundMeta.setI18nKey("route.404");
        notFoundMeta.setConstant(true);
        notFoundMeta.setHideInMenu(true);
        notFound.setMeta(notFoundMeta);
        routes.add(notFound);
        
        // 500
        RouteVO serverError = new RouteVO();
        serverError.setName("500");
        serverError.setPath("/500");
        serverError.setComponent("layout.blank$view.500");
        serverError.setId("500");
        RouteVO.RouteMeta serverErrorMeta = new RouteVO.RouteMeta();
        serverErrorMeta.setTitle("500");
        serverErrorMeta.setI18nKey("route.500");
        serverErrorMeta.setConstant(true);
        serverErrorMeta.setHideInMenu(true);
        serverError.setMeta(serverErrorMeta);
        routes.add(serverError);
        
        return Result.success(routes);
    }

    @Operation(summary = "获取用户路由")
    @SaCheckLogin
    @GetMapping("/getUserRoutes")
    public Result<UserRouteVO> getUserRoutes() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserRouteVO result = menuService.getUserRoutes(userId);
        return Result.success(result);
    }

    @Operation(summary = "检查路由是否存在")
    @GetMapping("/isRouteExist")
    public Result<Boolean> isRouteExist(@RequestParam String routeName) {
        // 简单实现：检查是否为已知路由
        boolean exists = "home".equals(routeName) || routeName.startsWith("manage_");
        return Result.success(exists);
    }
}
