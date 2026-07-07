package com.scaffold.modules.dish.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 菜品查询 DTO
 *
 * @author Henfon
 */
@Data
public class DishQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属分类ID
     */
    private Long categoryId;

    /**
     * 菜品名称（模糊搜索）
     */
    private String name;

    /**
     * 状态（0下架 1上架）
     */
    private Integer status;
}
