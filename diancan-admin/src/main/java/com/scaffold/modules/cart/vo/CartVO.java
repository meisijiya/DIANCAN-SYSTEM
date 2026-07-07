package com.scaffold.modules.cart.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车 VO
 *
 * @author Henfon
 */
@Data
public class CartVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 桌台ID
     */
    private Long tableId;

    /**
     * 购物车项列表
     */
    private List<CartItemVO> items;

    /**
     * 菜品总数
     */
    private Integer totalCount;

    /**
     * 总价
     */
    private BigDecimal totalPrice;
}
