package com.scaffold.modules.order.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 VO
 *
 * @author Henfon
 */
@Data
public class OrderVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String orderNo;

    private Long tableId;

    private String tableCode;

    /**
     * 桌次编码
     */
    private String tableSessionCode;

    /**
     * 桌台区域名称
     */
    private String areaName;

    private BigDecimal originalAmount;

    private BigDecimal discountRate;

    private Long couponId;

    private String couponName;

    private Integer couponType;

    private BigDecimal couponThresholdAmount;

    private BigDecimal couponDiscountAmount;

    private BigDecimal couponDiscountRate;

    private BigDecimal actualAmount;

    private Integer pointsUsed;

    private BigDecimal pointsDiscountAmount;

    private BigDecimal paidAmount;

    /**
     * 状态（0待支付 1已支付 2已取消 3已退款）
     */
    private Integer status;

    /**
     * 支付模式（0餐前付 1餐后付）
     */
    private Integer paymentMode;

    /**
     * 支付类型（0微信 1支付宝 2现金）
     */
    private Integer paymentMethod;

    /**
     * 订单类型（0堂食 1外卖）
     */
    private Integer orderType;

    private String remark;

    private String customerOpenid;

    private LocalDateTime createTime;

    /**
     * 订单项列表
     */
    private List<OrderItemVO> items;
}
