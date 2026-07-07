package com.scaffold.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.system.dto.RoleCreateDTO;
import com.scaffold.modules.system.dto.RoleQueryDTO;
import com.scaffold.modules.system.dto.RoleUpdateDTO;
import com.scaffold.modules.system.service.SysRoleService;
import com.scaffold.modules.system.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 *
 * @author Henfon
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @Operation(summary = "创建角色")
    @PostMapping
    @SaCheckPermission("system:role:add")
    public Result<Void> create(@Valid @RequestBody RoleCreateDTO dto) {
        roleService.createRole(dto);
        return Result.success();
    }

    @Operation(summary = "更新角色")
    @PutMapping
    @SaCheckPermission("system:role:edit")
    public Result<Void> update(@Valid @RequestBody RoleUpdateDTO dto) {
        roleService.updateRole(dto);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{roleId}")
    @SaCheckPermission("system:role:delete")
    public Result<Void> delete(@Parameter(description = "角色ID") @PathVariable Long roleId) {
        roleService.deleteRole(roleId);
        return Result.success();
    }


    @Operation(summary = "分页查询角色")
    @GetMapping("/page")
    @SaCheckPermission("system:role:list")
    public Result<PageResult<RoleVO>> page(RoleQueryDTO dto) {
        return Result.success(roleService.pageList(dto));
    }

    @Operation(summary = "获取所有角色列表")
    @GetMapping("/list")
    @SaCheckPermission("system:role:list")
    public Result<List<RoleVO>> list() {
        return Result.success(roleService.listAll());
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{roleId}")
    @SaCheckPermission("system:role:list")
    public Result<RoleVO> getInfo(@Parameter(description = "角色ID") @PathVariable Long roleId) {
        return Result.success(cn.hutool.core.bean.BeanUtil.copyProperties(
                roleService.getById(roleId), RoleVO.class));
    }

    @Operation(summary = "分配角色权限")
    @PostMapping("/{roleId}/menus")
    @SaCheckPermission("system:role:edit")
    public Result<Void> assignMenus(
            @Parameter(description = "角色ID") @PathVariable Long roleId,
            @RequestBody List<Long> menuIds) {
        roleService.assignMenus(roleId, menuIds);
        return Result.success();
    }

    @Operation(summary = "获取角色权限ID列表")
    @GetMapping("/{roleId}/menus")
    @SaCheckPermission("system:role:list")
    public Result<List<Long>> getRoleMenuIds(@Parameter(description = "角色ID") @PathVariable Long roleId) {
        return Result.success(roleService.getRoleMenuIds(roleId));
    }

    @Operation(summary = "更新角色状态")
    @PutMapping("/{roleId}/status/{status}")
    @SaCheckPermission("system:role:edit")
    public Result<Void> updateStatus(
            @Parameter(description = "角色ID") @PathVariable Long roleId,
            @Parameter(description = "状态") @PathVariable Integer status) {
        roleService.updateStatus(roleId, status);
        return Result.success();
    }

    @Operation(summary = "分配角色用户")
    @PostMapping("/{roleId}/users")
    @SaCheckPermission("system:role:edit")
    public Result<Void> assignUsers(
            @Parameter(description = "角色ID") @PathVariable Long roleId,
            @RequestBody List<Long> userIds) {
        roleService.assignUsers(roleId, userIds);
        return Result.success();
    }

    @Operation(summary = "获取角色已分配的用户ID列表")
    @GetMapping("/{roleId}/users")
    @SaCheckPermission("system:role:list")
    public Result<List<Long>> getRoleUserIds(@Parameter(description = "角色ID") @PathVariable Long roleId) {
        return Result.success(roleService.getRoleUserIds(roleId));
    }
}
