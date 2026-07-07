package com.scaffold.modules.audit.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.audit.dto.AuditLogQueryDTO;
import com.scaffold.modules.audit.dto.AuditLogExportTaskQueryDTO;
import com.scaffold.modules.audit.service.AuditLogService;
import com.scaffold.modules.audit.vo.AuditLogVO;
import com.scaffold.modules.audit.vo.AuditLogExportTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 审计日志控制器（管理端）
 *
 * @author Henfon
 */
@Tag(name = "审计日志（管理端）")
@RestController
@RequestMapping("/admin/audit-log")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @Operation(summary = "查询审计日志列表")
    @GetMapping("/list")
    public Result<PageResult<AuditLogVO>> listAuditLogs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize,
            AuditLogQueryDTO queryDTO) {
        return Result.success(auditLogService.listAuditLogs(pageNum, pageSize, queryDTO));
    }

    /**
     * 提交审计日志导出任务
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 导出改为异步任务模式，前端据此查询状态并下载结果文件。
     * @param queryDTO 查询条件
     * @return 导出任务
     */
    @Operation(summary = "提交审计日志导出任务")
    @PostMapping("/export/task")
    @SaCheckPermission("audit:list")
    public Result<AuditLogExportTaskVO> submitExportTask(@RequestBody(required = false) AuditLogQueryDTO queryDTO) {
        return Result.success(auditLogService.submitExportTask(queryDTO));
    }

    /**
     * 分页查询审计日志导出任务
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 查询当前登录后台用户创建的导出任务列表。
     * @param queryDTO 查询条件
     * @return 任务分页
     */
    @Operation(summary = "分页查询审计日志导出任务")
    @GetMapping("/export/task/page")
    @SaCheckPermission("audit:list")
    public Result<PageResult<AuditLogExportTaskVO>> pageExportTasks(AuditLogExportTaskQueryDTO queryDTO) {
        return Result.success(auditLogService.pageExportTasks(queryDTO));
    }

    /**
     * 下载审计日志导出任务结果文件
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 导出任务成功后下载对应 Excel 文件。
     * @param taskId 任务ID
     * @param response HTTP 响应
     */
    @Operation(summary = "下载审计日志导出任务文件")
    @GetMapping("/export/task/{taskId}/download")
    @SaCheckPermission("audit:list")
    public void downloadExportTaskFile(@Parameter(description = "任务ID") @PathVariable Long taskId,
                                       HttpServletResponse response) {
        auditLogService.downloadExportTaskFile(taskId, response);
    }
}
