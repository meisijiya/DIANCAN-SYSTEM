package com.scaffold.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.result.Result;
import com.scaffold.modules.system.dto.MenuCreateDTO;
import com.scaffold.modules.system.dto.MenuUpdateDTO;
import com.scaffold.modules.system.service.SysMenuService;
import com.scaffold.modules.system.vo.MenuTreeVO;
import com.scaffold.modules.system.vo.MenuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制器
 *
 * @author Henfon
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    @Operation(summary = "创建菜单")
    @PostMapping
    @SaCheckPermission("system:menu:add")
    public Result<Void> create(@Valid @RequestBody MenuCreateDTO dto) {
        menuService.createMenu(dto);
        return Result.success();
    }

    @Operation(summary = "更新菜单")
    @PutMapping
    @SaCheckPermission("system:menu:edit")
    public Result<Void> update(@Valid @RequestBody MenuUpdateDTO dto) {
        menuService.updateMenu(dto);
        return Result.success();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{menuId}")
    @SaCheckPermission("system:menu:delete")
    public Result<Void> delete(@Parameter(description = "菜单ID") @PathVariable Long menuId) {
        menuService.deleteMenu(menuId);
        return Result.success();
    }

    @Operation(summary = "获取菜单列表")
    @GetMapping("/list")
    @SaCheckPermission("system:menu:list")
    public Result<List<MenuVO>> list() {
        return Result.success(menuService.listAll());
    }

    @Operation(summary = "获取菜单树")
    @GetMapping("/tree")
    @SaCheckPermission("system:menu:list")
    public Result<List<MenuTreeVO>> tree() {
        return Result.success(menuService.getMenuTree());
    }

    @Operation(summary = "获取权限树")
    @GetMapping("/permission/tree")
    @SaCheckPermission("system:menu:list")
    public Result<List<MenuTreeVO>> permissionTree() {
        return Result.success(menuService.getPermissionTree());
    }

    @Operation(summary = "获取当前用户菜单树")
    @GetMapping("/user/tree")
    public Result<List<MenuTreeVO>> userTree() {
        return Result.success(menuService.getCurrentUserMenuTree());
    }

    @Operation(summary = "获取菜单详情")
    @GetMapping("/{menuId}")
    @SaCheckPermission("system:menu:list")
    public Result<MenuVO> getInfo(@Parameter(description = "菜单ID") @PathVariable Long menuId) {
        return Result.success(cn.hutool.core.bean.BeanUtil.copyProperties(
                menuService.getById(menuId), MenuVO.class));
    }
}
