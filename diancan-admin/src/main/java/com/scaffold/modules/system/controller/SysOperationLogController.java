package com.scaffold.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.system.dto.OperationLogQueryDTO;
import com.scaffold.modules.system.service.SysOperationLogService;
import com.scaffold.modules.system.vo.OperationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志管理控制器
 *
 * @author Henfon
 */
@Tag(name = "操作日志管理")
@RestController
@RequestMapping("/system/log/operation")
@RequiredArgsConstructor
public class SysOperationLogController {

    private final SysOperationLogService operationLogService;

    @Operation(summary = "分页查询操作日志")
    @GetMapping("/page")
    @SaCheckPermission("log:operation:list")
    public Result<PageResult<OperationLogVO>> page(OperationLogQueryDTO dto) {
        return Result.success(operationLogService.pageList(dto));
    }
}
