package com.scaffold.framework.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.scaffold.modules.system.service.PermissionCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Sa-Token 权限认证接口实现
 * <p>
 * 权限加载策略：
 * 1. 优先从数据库（通过 PermissionCacheService）加载用户的角色和权限
 * 2. 同时根据用户角色合并 RolePermissionDefaults 中的默认权限
 * 3. 两者取并集，确保预置角色的默认权限始终生效
 *
 * @author Henfon
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final PermissionCacheService permissionCacheService;

    /**
     * 获取用户权限列表
     * <p>
     * 合并数据库配置的权限和角色默认权限
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());

        // 从数据库加载权限
        List<String> dbPermissions = permissionCacheService.getUserPermissions(userId);

        // 从用户角色获取默认权限
        List<String> roles = permissionCacheService.getUserRoles(userId);
        List<String> defaultPermissions = RolePermissionDefaults.getMergedPermissions(roles);

        // 合并去重
        Set<String> merged = new LinkedHashSet<>(dbPermissions);
        merged.addAll(defaultPermissions);
        return new ArrayList<>(merged);
    }

    /**
     * 获取用户角色列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return permissionCacheService.getUserRoles(Long.valueOf(loginId.toString()));
    }
}
