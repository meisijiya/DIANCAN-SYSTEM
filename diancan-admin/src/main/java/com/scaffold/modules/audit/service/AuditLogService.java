package com.scaffold.modules.audit.service;

import com.scaffold.common.result.PageResult;
import com.scaffold.modules.audit.dto.AuditLogQueryDTO;
import com.scaffold.modules.audit.dto.AuditLogExportTaskQueryDTO;
import com.scaffold.modules.audit.vo.AuditLogVO;
import com.scaffold.modules.audit.vo.AuditLogExportTaskVO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 审计日志服务接口
 *
 * @author Henfon
 */
public interface AuditLogService {

    /**
     * 分页查询审计日志
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<AuditLogVO> listAuditLogs(int pageNum, int pageSize, AuditLogQueryDTO queryDTO);

    /**
     * 提交审计日志导出任务
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 创建导出任务后异步生成 Excel 文件，避免同步导出阻塞接口。
     * @param queryDTO 查询条件
     * @return 导出任务
     */
    AuditLogExportTaskVO submitExportTask(AuditLogQueryDTO queryDTO);

    /**
     * 分页查询导出任务
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 仅返回当前登录后台用户自己创建的审计日志导出任务。
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<AuditLogExportTaskVO> pageExportTasks(AuditLogExportTaskQueryDTO queryDTO);

    /**
     * 下载导出任务结果文件
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 校验任务归属和状态后输出 Excel 文件流。
     * @param taskId 任务ID
     * @param response HTTP 响应
     */
    void downloadExportTaskFile(Long taskId, HttpServletResponse response);
}
