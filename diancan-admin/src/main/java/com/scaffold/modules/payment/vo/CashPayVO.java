package com.scaffold.modules.payment.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 现金支付 VO（含找零金额）
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CashPayVO extends PaymentVO {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 找零金额
     */
    private BigDecimal changeAmount;
}
