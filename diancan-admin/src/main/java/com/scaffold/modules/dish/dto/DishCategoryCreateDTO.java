package com.scaffold.modules.dish.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜品分类创建 DTO
 *
 * @author Henfon
 */
@Data
public class DishCategoryCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过50")
    private String name;

    /**
     * 排序序号
     */
    private Integer sort = 0;

    /**
     * 状态（0停用 1启用）
     */
    private Integer status = 1;

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
