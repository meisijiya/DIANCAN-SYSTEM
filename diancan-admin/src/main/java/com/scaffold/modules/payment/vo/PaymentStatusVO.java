package com.scaffold.modules.payment.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 支付状态查询 VO
 *
 * @author Henfon
 */
@Data
public class PaymentStatusVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 支付流水号
     */
    private String paymentNo;

    /**
     * 状态（0待支付 1已支付 2已退款 3支付失败）
     */
    private Integer status;

    /**
     * 已支付金额
     */
    private BigDecimal paidAmount;

    /**
     * 剩余待支付金额
     */
    private BigDecimal remainingAmount;
}
