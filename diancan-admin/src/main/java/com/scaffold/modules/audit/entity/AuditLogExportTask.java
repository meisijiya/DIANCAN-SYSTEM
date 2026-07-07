package com.scaffold.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 审计日志导出任务实体
 *
 * @author Henfon
 */
@Data
@TableName("audit_log_export_task")
@EqualsAndHashCode(callSuper = true)
public class AuditLogExportTask extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务状态
     */
    private Integer taskStatus;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 操作人
     */
    private String operatorName;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 总记录数
     */
    private Integer totalCount;

    /**
     * 已导出记录数
     */
    private Integer exportedCount;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 最后错误
     */
    private String lastError;

    /**
     * 开始时间
     */
    private LocalDateTime startedTime;

    /**
     * 完成时间
     */
    private LocalDateTime finishedTime;
}
