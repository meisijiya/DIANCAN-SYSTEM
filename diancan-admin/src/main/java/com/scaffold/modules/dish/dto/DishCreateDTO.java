package com.scaffold.modules.dish.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜品创建 DTO
 *
 * @author Henfon
 */
@Data
public class DishCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    /**
     * 菜品名称
     */
    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 100, message = "菜品名称长度不能超过100")
    private String name;

    /**
     * 价格
     */
    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    /**
     * 图片URL
     */
    @Size(max = 500, message = "图片URL长度不能超过500")
    private String image;

    /**
     * 缩略图URL
     */
    @Size(max = 500, message = "缩略图URL长度不能超过500")
    private String thumbnail;

    /**
     * 辣度标记（0不辣 1微辣 2中辣 3重辣）
     */
    private Integer spiceLevel;

    /**
     * 配料列表（JSON数组）
     */
    @Size(max = 500, message = "配料列表长度不能超过500")
    private String ingredients;

    /**
     * 简介
     */
    @Size(max = 500, message = "简介长度不能超过500")
    private String description;

    /**
     * 库存数量（-1表示不限库存）
     */
    private Integer stock = -1;

    /**
     * 预设制作时限（分钟）
     */
    private Integer preparationTime;

    /**
     * 菜品规格项
     */
    private List<DishSpecItemDTO> specItems = new ArrayList<>();
}
