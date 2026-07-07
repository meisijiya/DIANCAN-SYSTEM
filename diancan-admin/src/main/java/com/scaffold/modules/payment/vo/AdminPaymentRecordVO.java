package com.scaffold.modules.payment.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端支付记录 VO
 */
@Data
public class AdminPaymentRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private String orderNo;
    private String tableCode;
    private String areaName;
    private String paymentNo;
    private String thirdPartyNo;
    private Integer paymentMethod;
    private BigDecimal amount;
    private Integer status;
    private String payerOpenid;
    private String payerName;
    private LocalDateTime createTime;
}
