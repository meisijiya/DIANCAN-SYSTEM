package com.scaffold.modules.audit.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志 VO
 *
 * @author Henfon
 */
@Data
@Schema(description = "审计日志")
public class AuditLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "日志ID")
    @ExcelProperty("日志ID")
    private Long id;

    @Schema(description = "关联订单ID")
    @ExcelProperty("订单ID")
    private Long orderId;

    @Schema(description = "关联订单项ID")
    @ExcelProperty("订单项ID")
    private Long orderItemId;

    @Schema(description = "操作类型")
    @ExcelProperty("操作类型")
    private String operationType;

    @Schema(description = "操作人ID")
    @ExcelProperty("操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    @ExcelProperty("操作人")
    private String operatorName;

    @Schema(description = "操作原因")
    @ExcelProperty("操作原因")
    private String reason;

    @Schema(description = "操作详情")
    @ExcelProperty("操作详情")
    private String detail;

    @Schema(description = "操作时间")
    @ExcelProperty("操作时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
