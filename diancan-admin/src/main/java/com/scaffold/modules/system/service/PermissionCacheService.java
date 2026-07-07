package com.scaffold.modules.system.service;

import java.util.List;

/**
 * 权限缓存服务接口
 *
 * @author Henfon
 */
public interface PermissionCacheService {

    /**
     * 缓存用户权限
     *
     * @param userId 用户ID
     */
    void cacheUserPermissions(Long userId);

    /**
     * 获取用户角色列表（从缓存）
     *
     * @param userId 用户ID
     * @return 角色编码列表
     */
    List<String> getUserRoles(Long userId);

    /**
     * 获取用户权限列表（从缓存）
     *
     * @param userId 用户ID
     * @return 权限标识列表
     */
    List<String> getUserPermissions(Long userId);

    /**
     * 清除用户权限缓存
     *
     * @param userId 用户ID
     */
    void clearUserCache(Long userId);

    /**
     * 清除角色下所有用户的权限缓存
     *
     * @param roleId 角色ID
     */
    void clearRoleUsersCache(Long roleId);

    /**
     * 刷新所有用户权限缓存
     */
    void refreshAllCache();
}
