package com.scaffold.modules.dish.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜品分类更新 DTO
 *
 * @author Henfon
 */
@Data
public class DishCategoryUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long id;

    /**
     * 分类名称
     */
    @Size(max = 50, message = "分类名称长度不能超过50")
    private String name;

    /**
     * 排序序号
     */
    private Integer sort;

    /**
     * 状态（0停用 1启用）
     */
    private Integer status;

    /**
     * 分类图片
     */
    @Size(max = 500, message = "分类图片长度不能超过500")
    private String image;

    /**
     * 默认规格组ID列表
     */
    private List<Long> specGroupIds = new ArrayList<>();

}
