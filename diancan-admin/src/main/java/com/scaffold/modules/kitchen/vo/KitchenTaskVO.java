package com.scaffold.modules.kitchen.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后厨任务 VO
 *
 * @author Henfon
 */
@Data
public class KitchenTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单项ID */
    private Long id;

    /** 所属订单ID */
    private Long orderId;

    /** 订单编号 */
    private String orderNo;

    /** 桌台编号 */
    private String tableCode;

    /** 桌台区域名称 */
    private String areaName;

    /** 支付模式（0餐前付 1餐后付） */
    private Integer paymentMode;

    /** 菜品ID */
    private Long dishId;

    /** 菜品名称 */
    private String dishName;

    /** 菜品图片 */
    private String dishImage;

    /** 数量 */
    private Integer quantity;

    /** 口味备注 */
    private String remark;

    /** 状态（0待制作 1制作中 2已完成） */
    private Integer status;

    /** 加入订单时间 */
    private LocalDateTime addedAt;

    /** 预设制作时限（分钟，来自菜品表） */
    private Integer preparationTime;

    /** 是否超时 */
    private Boolean overtime;
}
