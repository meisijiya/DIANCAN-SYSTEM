package com.scaffold.modules.order.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理端订单试算 VO
 *
 * @author Henfon
 */
@Data
public class AdminOrderEstimateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private BigDecimal originalAmount = BigDecimal.ZERO;

    private BigDecimal memberDiscountAmount = BigDecimal.ZERO;

    private BigDecimal couponDiscountAmount = BigDecimal.ZERO;

    private BigDecimal pointsDiscountAmount = BigDecimal.ZERO;

    private BigDecimal discountAmount = BigDecimal.ZERO;

    private BigDecimal payableAmount = BigDecimal.ZERO;

    private Long couponId;

    private String couponName;

    private Integer requestedPoints = 0;

    private Integer actualUsedPoints = 0;

    private Integer availablePoints = 0;

    private Integer maxUsablePoints = 0;

    private List<AdminOrderEstimateItemVO> items = new ArrayList<>();

    private List<String> tips = new ArrayList<>();

    /**
     * 订单试算项 VO
     */
    @Data
    public static class AdminOrderEstimateItemVO implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long dishId;

        private String dishName;

        private BigDecimal unitPrice = BigDecimal.ZERO;

        private Integer quantity = 0;

        private BigDecimal amount = BigDecimal.ZERO;
    }
}
