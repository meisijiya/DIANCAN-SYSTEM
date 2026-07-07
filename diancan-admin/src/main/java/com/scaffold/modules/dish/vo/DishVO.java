package com.scaffold.modules.dish.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜品详情 VO
 *
 * @author Henfon
 */
@Data
public class DishVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 菜品ID
     */
    private Long id;

    /**
     * 所属分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 菜品名称
     */
    private String name;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 图片URL
     */
    private String image;

    /**
     * 缩略图URL
     */
    private String thumbnail;

    /**
     * 辣度标记（0不辣 1微辣 2中辣 3重辣）
     */
    private Integer spiceLevel;

    /**
     * 配料列表（JSON数组）
     */
    private String ingredients;

    /**
     * 简介
     */
    private String description;

    /**
     * 状态（0下架 1上架）
     */
    private Integer status;

    /**
     * 是否售罄（0否 1是）
     */
    private Integer soldOut;

    /**
     * 库存数量（-1表示不限库存）
     */
    private Integer stock;

    /**
     * 预设制作时限（分钟）
     */
    private Integer preparationTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 菜品规格项
     */
    private List<DishSpecItemVO> specItems = new ArrayList<>();
}
