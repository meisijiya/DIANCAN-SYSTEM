package com.scaffold.modules.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 支付记录实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_record")
public class PaymentRecord extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 支付流水号
     */
    private String paymentNo;

    /**
     * 第三方支付流水号
     */
    private String thirdPartyNo;

    /**
     * 支付方式（0微信 1支付宝 2现金）
     */
    private Integer paymentMethod;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 状态（0待支付 1已支付 2已退款 3支付失败）
     */
    private Integer status;

    /**
     * 支付人openid
     */
    private String payerOpenid;

    /**
     * 支付回调原始数据
     */
    private String callbackData;
}
