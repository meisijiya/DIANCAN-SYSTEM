package com.scaffold.modules.audit.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 审计日志导出任务查询 DTO
 *
 * @author Henfon
 */
@Data
public class AuditLogExportTaskQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务状态
     */
    private Integer taskStatus;

    /**
     * 页码
     */
    private Long pageNum = 1L;

    /**
     * 每页数量
     */
    private Long pageSize = 10L;
}
