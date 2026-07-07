package com.scaffold.modules.report.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 菜品销售排行 VO
 *
 * @author Henfon
 */
@Data
public class DishRankingVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 菜品ID
     */
    private Long dishId;

    /**
     * 菜品名称
     */
    private String dishName;

    /**
     * 总销量
     */
    private Integer totalQuantity;

    /**
     * 总销售额
     */
    private BigDecimal totalAmount;
}
