package com.scaffold.modules.dish.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 菜品列表 VO（精简字段，用于列表展示）
 *
 * @author Henfon
 */
@Data
public class DishListVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 菜品ID
     */
    private Long id;

    /**
     * 菜品名称
     */
    private String name;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 缩略图URL
     */
    private String thumbnail;

    /**
     * 是否售罄（0否 1是）
     */
    private Integer soldOut;
}
