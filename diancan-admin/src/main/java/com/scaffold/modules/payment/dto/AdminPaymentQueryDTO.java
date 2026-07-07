package com.scaffold.modules.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 管理端支付记录查询 DTO
 */
@Data
@Schema(description = "支付记录查询参数")
public class AdminPaymentQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "支付流水号")
    private String paymentNo;

    @Schema(description = "支付方式（0微信 1支付宝 2现金）")
    private Integer paymentMethod;

    @Schema(description = "支付状态（0待支付 1已支付 2已退款 3支付失败）")
    private Integer status;

    @Schema(description = "区域名称")
    private String areaName;

    @Schema(description = "开始日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
