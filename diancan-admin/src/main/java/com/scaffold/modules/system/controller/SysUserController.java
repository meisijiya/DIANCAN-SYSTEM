package com.scaffold.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.system.dto.AdminResetPasswordDTO;
import com.scaffold.modules.system.dto.PasswordUpdateDTO;
import com.scaffold.modules.system.dto.UserQueryDTO;
import com.scaffold.modules.system.dto.UserUpdateDTO;
import com.scaffold.modules.system.service.SysUserService;
import com.scaffold.modules.system.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 *
 * @author Henfon
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @Operation(summary = "分页查询用户列表")
    @SaCheckPermission("system:user:list")
    @GetMapping("/page")
    public Result<PageResult<UserVO>> page(UserQueryDTO dto) {
        return Result.success(userService.pageList(dto));
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UserUpdateDTO dto) {
        userService.updateUserInfo(dto);
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        userService.updatePassword(dto);
        return Result.success();
    }

    @Operation(summary = "管理员重置用户密码")
    @SaCheckPermission("system:user:edit")
    @PutMapping("/{userId}/password/reset")
    public Result<Void> resetPassword(@PathVariable Long userId, @Valid @RequestBody AdminResetPasswordDTO dto) {
        userService.resetPassword(userId, dto.getNewPassword());
        return Result.success();
    }

    @Operation(summary = "启用/禁用用户")
    @SaCheckPermission("system:user:edit")
    @PutMapping("/{userId}/status/{status}")
    public Result<Void> updateStatus(@PathVariable Long userId, @PathVariable Integer status) {
        userService.updateStatus(userId, status);
        return Result.success();
    }
}
