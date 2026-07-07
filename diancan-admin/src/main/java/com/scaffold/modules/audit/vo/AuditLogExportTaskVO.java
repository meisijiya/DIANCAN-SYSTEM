package com.scaffold.modules.audit.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 审计日志导出任务 VO
 *
 * @author Henfon
 */
@Data
public class AuditLogExportTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer taskStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private String operatorName;
    private String operationType;
    private Integer totalCount;
    private Integer exportedCount;
    private String fileName;
    private String filePath;
    private String lastError;
    private LocalDateTime startedTime;
    private LocalDateTime finishedTime;
    private LocalDateTime createTime;
}
