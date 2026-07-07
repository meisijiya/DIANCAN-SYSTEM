package com.scaffold.modules.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 审计日志查询 DTO
 *
 * @author Henfon
 */
@Data
@Schema(description = "审计日志查询参数")
public class AuditLogQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "开始日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "操作类型（RETURN/REPLACE/GIFT/DISCOUNT/RUSH）")
    private String operationType;
}
