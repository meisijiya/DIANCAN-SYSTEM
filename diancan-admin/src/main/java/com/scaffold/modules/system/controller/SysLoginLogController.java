package com.scaffold.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.system.dto.LoginLogQueryDTO;
import com.scaffold.modules.system.service.SysLoginLogService;
import com.scaffold.modules.system.vo.LoginLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录日志管理控制器
 *
 * @author Henfon
 */
@Tag(name = "登录日志管理")
@RestController
@RequestMapping("/system/log/login")
@RequiredArgsConstructor
public class SysLoginLogController {

    private final SysLoginLogService loginLogService;

    @Operation(summary = "分页查询登录日志")
    @GetMapping("/page")
    @SaCheckPermission("log:login:list")
    public Result<PageResult<LoginLogVO>> page(LoginLogQueryDTO dto) {
        return Result.success(loginLogService.pageList(dto));
    }
}
