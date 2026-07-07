package com.scaffold.modules.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 菜品实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dish")
public class Dish extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属分类ID
     */
    private Long categoryId;

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
     * 扩展规格值(JSON)
     */
    private String specValues;

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
}
