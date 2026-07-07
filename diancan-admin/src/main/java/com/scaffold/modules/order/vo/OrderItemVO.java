package com.scaffold.modules.order.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项 VO
 *
 * @author Henfon
 */
@Data
public class OrderItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orderId;

    private Long dishId;

    private String dishName;

    private String dishImage;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal amount;

    private String remark;

    /**
     * 状态（0待制作 1制作中 2已完成）
     */
    private Integer status;

    /**
     * 支付状态（0未支付 2已支付）
     */
    private Integer paymentStatus;

    /**
     * 是否赠送（0否 1是）
     */
    private Integer isGift;

    private LocalDateTime addedAt;
}
