package com.scaffold.modules.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.common.enums.StatusEnum;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.system.dto.RoleCreateDTO;
import com.scaffold.modules.system.dto.RoleQueryDTO;
import com.scaffold.modules.system.dto.RoleUpdateDTO;
import com.scaffold.modules.system.entity.SysRole;
import com.scaffold.modules.system.mapper.SysRoleMapper;
import com.scaffold.modules.system.mapper.SysRoleMenuMapper;
import com.scaffold.modules.system.mapper.SysUserRoleMapper;
import com.scaffold.modules.system.service.PermissionCacheService;
import com.scaffold.modules.system.service.SysRoleService;
import com.scaffold.modules.system.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PermissionCacheService permissionCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(RoleCreateDTO dto) {
        // 检查角色编码是否存在
        if (existsByCode(dto.getCode())) {
            throw new BusinessException(ResultCode.ROLE_CODE_EXISTS);
        }

        SysRole role = new SysRole();
        BeanUtil.copyProperties(dto, role);
        save(role);
        log.info("角色创建成功: {}", dto.getCode());
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleUpdateDTO dto) {
        SysRole existRole = getById(dto.getId());
        if (existRole == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 检查角色编码是否被其他角色使用
        if (!existRole.getCode().equals(dto.getCode()) && existsByCode(dto.getCode())) {
            throw new BusinessException(ResultCode.ROLE_CODE_EXISTS);
        }

        SysRole role = new SysRole();
        BeanUtil.copyProperties(dto, role);
        updateById(role);
        log.info("角色更新成功: {}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        SysRole role = getById(roleId);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 检查角色是否被用户使用
        int userCount = userRoleMapper.countByRoleId(roleId);
        if (userCount > 0) {
            throw new BusinessException(ResultCode.ROLE_IN_USE);
        }

        // 删除角色
        removeById(roleId);
        // 删除角色菜单关联
        roleMenuMapper.deleteByRoleId(roleId);
        log.info("角色删除成功: {}", roleId);
    }

    @Override
    public PageResult<RoleVO> pageList(RoleQueryDTO dto) {
        Page<SysRole> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(dto.getName()), SysRole::getName, dto.getName())
                .like(StrUtil.isNotBlank(dto.getCode()), SysRole::getCode, dto.getCode())
                .eq(dto.getStatus() != null, SysRole::getStatus, dto.getStatus())
                .orderByDesc(SysRole::getCreateTime);

        Page<SysRole> result = page(page, wrapper);
        List<RoleVO> voList = BeanUtil.copyToList(result.getRecords(), RoleVO.class);
        return PageResult.of(voList, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    public List<RoleVO> listAll() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, StatusEnum.ENABLED.getValue())
                .orderByAsc(SysRole::getId);
        List<SysRole> roles = list(wrapper);
        return BeanUtil.copyToList(roles, RoleVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = getById(roleId);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 删除原有关联
        roleMenuMapper.deleteByRoleId(roleId);

        // 批量插入新关联
        if (CollUtil.isNotEmpty(menuIds)) {
            roleMenuMapper.batchInsert(roleId, menuIds);
        }

        // 清除该角色下所有用户的权限缓存
        permissionCacheService.clearRoleUsersCache(roleId);
        log.info("角色权限分配成功: roleId={}, menuCount={}", roleId, menuIds != null ? menuIds.size() : 0);
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        return roleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    public void updateStatus(Long roleId, Integer status) {
        SysRole role = new SysRole();
        role.setId(roleId);
        role.setStatus(status);
        updateById(role);

        // 如果禁用角色，清除该角色下所有用户的权限缓存
        if (StatusEnum.DISABLED.getValue().equals(status)) {
            permissionCacheService.clearRoleUsersCache(roleId);
        }
        log.info("角色状态更新: roleId={}, status={}", roleId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUsers(Long roleId, List<Long> userIds) {
        SysRole role = getById(roleId);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 删除原有关联
        userRoleMapper.deleteByRoleId(roleId);

        // 批量插入新关联
        if (CollUtil.isNotEmpty(userIds)) {
            userRoleMapper.batchInsertByRoleId(roleId, userIds);
        }

        // 清除该角色下所有用户的权限缓存
        permissionCacheService.clearRoleUsersCache(roleId);
        log.info("角色用户分配成功: roleId={}, userCount={}", roleId, userIds != null ? userIds.size() : 0);
    }

    @Override
    public List<Long> getRoleUserIds(Long roleId) {
        return userRoleMapper.selectUserIdsByRoleId(roleId);
    }

    /**
     * 检查角色编码是否存在
     */
    private boolean existsByCode(String code) {
        return count(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, code)) > 0;
    }
}
