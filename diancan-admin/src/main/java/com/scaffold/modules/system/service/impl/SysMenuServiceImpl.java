package com.scaffold.modules.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.common.enums.MenuTypeEnum;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.ResultCode;
import com.scaffold.framework.satoken.RolePermissionDefaults;
import com.scaffold.modules.system.dto.MenuCreateDTO;
import com.scaffold.modules.system.dto.MenuUpdateDTO;
import com.scaffold.modules.system.entity.SysMenu;
import com.scaffold.modules.system.mapper.SysMenuMapper;
import com.scaffold.modules.system.service.PermissionCacheService;
import com.scaffold.modules.system.service.SysMenuService;
import com.scaffold.modules.system.vo.MenuTreeVO;
import com.scaffold.modules.system.vo.MenuVO;
import com.scaffold.modules.system.vo.RouteVO;
import com.scaffold.modules.system.vo.UserRouteVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    private final PermissionCacheService permissionCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createMenu(MenuCreateDTO dto) {
        SysMenu menu = new SysMenu();
        BeanUtil.copyProperties(dto, menu);
        save(menu);
        log.info("菜单创建成功: {}", dto.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(MenuUpdateDTO dto) {
        SysMenu existMenu = getById(dto.getId());
        if (existMenu == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 不能将自己设为父菜单
        if (dto.getId().equals(dto.getParentId())) {
            throw new BusinessException("不能将自己设为父菜单");
        }

        SysMenu menu = new SysMenu();
        BeanUtil.copyProperties(dto, menu);
        updateById(menu);
        log.info("菜单更新成功: {}", dto.getId());
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long menuId) {
        SysMenu menu = getById(menuId);
        if (menu == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 检查是否有子菜单
        int childCount = baseMapper.countByParentId(menuId);
        if (childCount > 0) {
            throw new BusinessException(ResultCode.MENU_HAS_CHILDREN);
        }

        removeById(menuId);
        log.info("菜单删除成功: {}", menuId);
    }

    @Override
    public List<MenuVO> listAll() {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysMenu::getOrderNum);
        List<SysMenu> menus = list(wrapper);
        return BeanUtil.copyToList(menus, MenuVO.class);
    }

    @Override
    public List<MenuTreeVO> getMenuTree() {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysMenu::getOrderNum);
        List<SysMenu> menus = list(wrapper);
        List<SysMenu> filteredMenus = menus.stream()
                .filter(m -> !MenuTypeEnum.BUTTON.getValue().equals(m.getType()))
                .collect(Collectors.toList());
        return buildTree(filteredMenus);
    }

    @Override
    public List<MenuTreeVO> getPermissionTree() {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysMenu::getOrderNum);
        List<SysMenu> menus = list(wrapper);
        return buildTree(menus);
    }

    @Override
    public List<MenuTreeVO> getCurrentUserMenuTree() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<SysMenu> menus = baseMapper.selectMenusByUserId(userId);
        List<SysMenu> filteredMenus = menus.stream()
                .filter(m -> !MenuTypeEnum.BUTTON.getValue().equals(m.getType()))
                .collect(Collectors.toList());
        return buildTree(filteredMenus);
    }

    @Override
    public List<String> getUserPermissions(Long userId) {
        return baseMapper.selectPermissionsByUserId(userId);
    }

    @Override
    public UserRouteVO getUserRoutes(Long userId) {
        List<SysMenu> menus = baseMapper.selectMenusByUserId(userId);
        // 只返回目录和菜单，不返回按钮
        List<SysMenu> filteredMenus = menus.stream()
                .filter(m -> !MenuTypeEnum.BUTTON.getValue().equals(m.getType()))
                .collect(Collectors.toList());
        
        // 构建路由树
        List<RouteVO> routes = buildRouteTree(filteredMenus);
        
        // 为订单模块注入订单详情子路由（隐藏菜单，不在数据库中管理）
        injectHiddenRoutes(routes);
        
        // 添加首页路由
        RouteVO homeRoute = new RouteVO();
        homeRoute.setName("home");
        homeRoute.setPath("/home");
        homeRoute.setComponent("layout.base$view.home");
        homeRoute.setId("home");
        RouteVO.RouteMeta homeMeta = new RouteVO.RouteMeta();
        homeMeta.setTitle("首页");
        homeMeta.setI18nKey("route.home");
        homeMeta.setIcon("mdi:monitor-dashboard");
        homeMeta.setOrder(0);
        homeRoute.setMeta(homeMeta);
        routes.add(0, homeRoute);
        
        UserRouteVO result = new UserRouteVO();
        result.setRoutes(routes);
        result.setHome(resolveHomeRoute(userId, routes));
        return result;
    }

    /**
     * 作者：Henfon
     * 日期：2026-07-04
     * 描述：根据用户角色与已授权路由解析后台登录后的首页，避免业务角色落到无权限看板页
     *
     * @param userId 用户ID
     * @param routes 当前用户可访问路由
     * @return 首页路由名称
     */
    private String resolveHomeRoute(Long userId, List<RouteVO> routes) {
        List<String> roleCodes = permissionCacheService.getUserRoles(userId);

        // 餐厅管理员保留经营首页，其余业务角色优先进入自己的工作台
        if (roleCodes.contains(RolePermissionDefaults.ROLE_ADMIN)) {
            return "home";
        }

        if (roleCodes.contains(RolePermissionDefaults.ROLE_WAITER)) {
            return pickFirstAvailableRoute(routes, List.of("service_table-board", "service_place-order", "service_order-ops"));
        }

        if (roleCodes.contains(RolePermissionDefaults.ROLE_CASHIER)) {
            return pickFirstAvailableRoute(routes, List.of("service_order-ops", "service_table-board", "service_place-order"));
        }

        if (roleCodes.contains(RolePermissionDefaults.ROLE_KITCHEN)) {
            return pickFirstAvailableRoute(routes, List.of("service_kitchen"));
        }

        return pickFirstAvailableRoute(routes, List.of("home"));
    }

    /**
     * 作者：Henfon
     * 日期：2026-07-04
     * 描述：从候选首页中选出当前用户已授权的第一个路由，若都不存在则回退到路由树中的第一个叶子节点
     *
     * @param routes 当前用户路由树
     * @param preferredRouteNames 候选首页名称列表
     * @return 可用首页路由名称
     */
    private String pickFirstAvailableRoute(List<RouteVO> routes, List<String> preferredRouteNames) {
        List<String> availableRouteNames = flattenLeafRouteNames(routes);
        for (String preferredRouteName : preferredRouteNames) {
            if (availableRouteNames.contains(preferredRouteName)) {
                return preferredRouteName;
            }
        }

        if (availableRouteNames.contains("home")) {
            return "home";
        }

        return CollUtil.isNotEmpty(availableRouteNames) ? availableRouteNames.get(0) : "home";
    }

    /**
     * 作者：Henfon
     * 日期：2026-07-04
     * 描述：提取路由树中的叶子节点名称，用于首页兜底和候选命中判断
     *
     * @param routes 当前用户路由树
     * @return 叶子路由名称列表
     */
    private List<String> flattenLeafRouteNames(List<RouteVO> routes) {
        List<String> routeNames = new ArrayList<>();
        collectLeafRouteNames(routes, routeNames);
        return routeNames;
    }

    /**
     * 作者：Henfon
     * 日期：2026-07-04
     * 描述：递归收集叶子路由名称
     *
     * @param routes 当前层路由
     * @param routeNames 收集结果
     */
    private void collectLeafRouteNames(List<RouteVO> routes, List<String> routeNames) {
        if (CollUtil.isEmpty(routes)) {
            return;
        }

        for (RouteVO route : routes) {
            if (CollUtil.isNotEmpty(route.getChildren())) {
                collectLeafRouteNames(route.getChildren(), routeNames);
                continue;
            }

            if (StrUtil.isNotBlank(route.getName())) {
                routeNames.add(route.getName());
            }
        }
    }

    /**
     * 构建路由树（适配Soybean Admin）
     */
    private List<RouteVO> buildRouteTree(List<SysMenu> menus) {
        if (CollUtil.isEmpty(menus)) {
            return new ArrayList<>();
        }

        // 找出所有根节点（一级菜单）
        List<SysMenu> rootMenus = menus.stream()
                .filter(m -> m.getParentId() == null || m.getParentId() == 0)
                .sorted((a, b) -> {
                    int orderA = a.getOrderNum() != null ? a.getOrderNum() : 0;
                    int orderB = b.getOrderNum() != null ? b.getOrderNum() : 0;
                    return orderA - orderB;
                })
                .collect(Collectors.toList());

        List<RouteVO> routes = new ArrayList<>();
        for (SysMenu rootMenu : rootMenus) {
            RouteVO route = convertToRoute(rootMenu, menus);
            if (route != null) {
                routes.add(route);
            }
        }

        return routes;
    }

    /**
     * 转换菜单为路由
     */
    private RouteVO convertToRoute(SysMenu menu, List<SysMenu> allMenus) {
        RouteVO route = new RouteVO();
        
        // 生成路由名称（使用下划线连接的格式，如 manage_user）
        String routeName = generateRouteName(menu);
        route.setName(routeName);
        route.setPath(menu.getPath());
        route.setId(String.valueOf(menu.getId()));
        
        // 设置meta
        RouteVO.RouteMeta meta = new RouteVO.RouteMeta();
        meta.setTitle(menu.getName());
        meta.setIcon(menu.getIcon());
        meta.setOrder(menu.getOrderNum());
        route.setMeta(meta);
        
        // 查找子菜单
        List<SysMenu> children = allMenus.stream()
                .filter(m -> menu.getId().equals(m.getParentId()))
                .sorted((a, b) -> {
                    int orderA = a.getOrderNum() != null ? a.getOrderNum() : 0;
                    int orderB = b.getOrderNum() != null ? b.getOrderNum() : 0;
                    return orderA - orderB;
                })
                .collect(Collectors.toList());
        
        if (CollUtil.isNotEmpty(children)) {
            // 有子菜单，这是一个目录
            route.setComponent("layout.base");
            List<RouteVO> childRoutes = new ArrayList<>();
            for (SysMenu child : children) {
                RouteVO childRoute = convertToRoute(child, allMenus);
                if (childRoute != null) {
                    childRoutes.add(childRoute);
                }
            }
            route.setChildren(childRoutes);
        } else {
            // 没有子菜单，这是一个页面
            // 组件格式：layout.base$view.xxx 或 view.xxx
            // 这里不依赖数据库 component 字段，避免“已授权但无组件信息”的菜单变成空路由节点
            if (menu.getParentId() == null || menu.getParentId() == 0) {
                route.setComponent("layout.base$view." + routeName);
            } else {
                route.setComponent("view." + routeName);
            }
        }

        // 兜底过滤：无组件且无子路由的节点直接丢弃，避免前端出现一级空菜单与 vue-router 警告
        if (StrUtil.isBlank(route.getComponent()) && CollUtil.isEmpty(route.getChildren())) {
            return null;
        }

        return route;
    }

    /**
     * 生成路由名称
     * 将路径转换为下划线格式，如 /system/user -> manage_user
     */
    private String generateRouteName(SysMenu menu) {
        String path = menu.getPath();
        if (StrUtil.isBlank(path)) {
            return "menu_" + menu.getId();
        }
        
        // 移除开头的斜杠，将斜杠替换为下划线
        String name = path.replaceFirst("^/", "").replace("/", "_");
        
        // 统一使用 manage 前缀
        // /system -> manage, /system/user -> manage_user
        // /log -> log, /log/login -> log_login
        if (name.startsWith("system_")) {
            name = "manage_" + name.substring("system_".length());
        } else if (name.equals("system")) {
            name = "manage";
        }
        
        return name;
    }

    /**
     * 为路由树注入隐藏路由（如订单详情页，不在菜单中管理但需要路由访问）
     */
    private void injectHiddenRoutes(List<RouteVO> routes) {
        for (RouteVO route : routes) {
            if ("order".equals(route.getName()) && route.getChildren() != null) {
                // 注入订单详情路由
                RouteVO detailRoute = new RouteVO();
                detailRoute.setName("order_detail");
                detailRoute.setPath("/order/detail/:id");
                detailRoute.setComponent("view.order_detail");
                detailRoute.setProps(true);
                detailRoute.setId("order_detail");
                RouteVO.RouteMeta detailMeta = new RouteVO.RouteMeta();
                detailMeta.setTitle("订单详情");
                detailMeta.setI18nKey("route.order_detail");
                detailMeta.setHideInMenu(true);
                detailMeta.setActiveMenu("order_list");
                detailRoute.setMeta(detailMeta);
                route.getChildren().add(detailRoute);
                break;
            }
        }
    }

    /**
     * 构建菜单树
     */
    private List<MenuTreeVO> buildTree(List<SysMenu> menus) {
        if (CollUtil.isEmpty(menus)) {
            return new ArrayList<>();
        }

        List<MenuTreeVO> voList = BeanUtil.copyToList(menus, MenuTreeVO.class);
        
        // 找出所有根节点
        List<MenuTreeVO> rootList = voList.stream()
                .filter(m -> (m.getParentId() == null || m.getParentId() == 0)
                        && !MenuTypeEnum.BUTTON.getValue().equals(m.getType()))
                .collect(Collectors.toList());

        // 递归设置子节点
        for (MenuTreeVO root : rootList) {
            setChildren(root, voList);
        }

        return rootList;
    }

    /**
     * 递归设置子节点
     */
    private void setChildren(MenuTreeVO parent, List<MenuTreeVO> allMenus) {
        List<MenuTreeVO> children = allMenus.stream()
                .filter(m -> parent.getId().equals(m.getParentId()))
                .collect(Collectors.toList());

        if (CollUtil.isNotEmpty(children)) {
            parent.setChildren(children);
            for (MenuTreeVO child : children) {
                setChildren(child, allMenus);
            }
        }
    }
}
