package com.scaffold.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.common.constant.CacheConstants;
import com.scaffold.framework.redis.RedisUtils;
import com.scaffold.modules.system.entity.SysUserRole;
import com.scaffold.modules.system.mapper.SysUserMapper;
import com.scaffold.modules.system.mapper.SysUserRoleMapper;
import com.scaffold.modules.system.service.PermissionCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限缓存服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheServiceImpl implements PermissionCacheService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final RedisUtils redisUtils;

    @Override
    public void cacheUserPermissions(Long userId) {
        List<String> roles = userMapper.selectRoleCodesByUserId(userId);
        List<String> permissions = userMapper.selectPermissionsByUserId(userId);
        
        redisUtils.set(CacheConstants.USER_ROLE_KEY + userId, roles, CacheConstants.CACHE_EXPIRE);
        redisUtils.set(CacheConstants.USER_PERMISSION_KEY + userId, permissions, CacheConstants.CACHE_EXPIRE);
        
        log.debug("缓存用户权限: userId={}, roles={}, permissions={}", userId, roles.size(), permissions.size());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getUserRoles(Long userId) {
        String cacheKey = CacheConstants.USER_ROLE_KEY + userId;
        Object cached = redisUtils.get(cacheKey);
        
        if (cached != null) {
            return (List<String>) cached;
        }
        
        // 缓存不存在，从数据库查询并缓存
        List<String> roles = userMapper.selectRoleCodesByUserId(userId);
        redisUtils.set(cacheKey, roles, CacheConstants.CACHE_EXPIRE);
        return roles;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getUserPermissions(Long userId) {
        String cacheKey = CacheConstants.USER_PERMISSION_KEY + userId;
        Object cached = redisUtils.get(cacheKey);
        
        if (cached != null) {
            return (List<String>) cached;
        }
        
        // 缓存不存在，从数据库查询并缓存
        List<String> permissions = userMapper.selectPermissionsByUserId(userId);
        redisUtils.set(cacheKey, permissions, CacheConstants.CACHE_EXPIRE);
        return permissions;
    }


    @Override
    public void clearUserCache(Long userId) {
        redisUtils.delete(CacheConstants.USER_ROLE_KEY + userId);
        redisUtils.delete(CacheConstants.USER_PERMISSION_KEY + userId);
        log.debug("清除用户权限缓存: userId={}", userId);
    }

    @Override
    public void clearRoleUsersCache(Long roleId) {
        // 查询该角色下的所有用户
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getRoleId, roleId);
        List<SysUserRole> userRoles = userRoleMapper.selectList(wrapper);
        
        // 清除每个用户的缓存
        for (SysUserRole userRole : userRoles) {
            clearUserCache(userRole.getUserId());
        }
        
        log.info("清除角色下用户权限缓存: roleId={}, userCount={}", roleId, userRoles.size());
    }

    @Override
    public void refreshAllCache() {
        // 获取所有用户角色关联，提取唯一用户ID
        List<SysUserRole> allUserRoles = userRoleMapper.selectList(null);
        List<Long> userIds = allUserRoles.stream()
                .map(SysUserRole::getUserId)
                .distinct()
                .toList();
        
        // 刷新每个用户的缓存
        for (Long userId : userIds) {
            cacheUserPermissions(userId);
        }
        
        log.info("刷新所有用户权限缓存完成: userCount={}", userIds.size());
    }
}
