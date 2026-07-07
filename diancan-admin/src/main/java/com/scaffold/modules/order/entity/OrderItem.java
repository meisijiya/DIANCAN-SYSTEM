package com.scaffold.modules.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_item")
public class OrderItem extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属订单ID
     */
    private Long orderId;

    /**
     * 菜品ID
     */
    private Long dishId;

    /**
     * 菜品名称（冗余）
     */
    private String dishName;

    /**
     * 菜品图片（冗余）
     */
    private String dishImage;

    /**
     * 下单时单价（冗余）
     */
    private BigDecimal price;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 小计金额
     */
    private BigDecimal amount;

    /**
     * 口味备注
     */
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

    /**
     * 加入订单时间（用于区分加菜）
     */
    private LocalDateTime addedAt;
}
