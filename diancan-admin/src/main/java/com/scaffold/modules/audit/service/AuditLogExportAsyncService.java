package com.scaffold.modules.audit.service;

/**
 * 审计日志异步导出服务接口
 *
 * @author Henfon
 */
public interface AuditLogExportAsyncService {

    /**
     * 异步执行审计日志导出
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 根据任务快照异步生成审计日志 Excel 文件，并持续回写任务状态。
     * @param taskId 任务ID
     */
    void exportAuditLogsAsync(Long taskId);
}
