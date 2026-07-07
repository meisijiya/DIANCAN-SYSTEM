package com.scaffold.modules.payment.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录 VO
 *
 * @author Henfon
 */
@Data
public class PaymentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orderId;

    private String paymentNo;

    private String thirdPartyNo;

    /**
     * 小程序支付 appId
     */
    private String appId;

    /**
     * 小程序调起支付时间戳
     */
    private String timeStamp;

    /**
     * 小程序调起支付随机串
     */
    private String nonceStr;

    /**
     * 小程序调起支付 package 值
     */
    private String packageValue;

    /**
     * 小程序调起支付签名类型
     */
    private String signType;

    /**
     * 小程序调起支付签名
     */
    private String paySign;

    /**
     * 跳转支付链接（移动端）
     */
    private String payUrl;

    /**
     * 二维码图片链接（PC端）
     */
    private String qrCodeUrl;

    /**
     * 支付方式（0微信 1支付宝 2现金）
     */
    private Integer paymentMethod;

    private BigDecimal amount;

    /**
     * 状态（0待支付 1已支付 2已退款 3支付失败）
     */
    private Integer status;

    private String payerOpenid;

    private LocalDateTime createTime;
}
