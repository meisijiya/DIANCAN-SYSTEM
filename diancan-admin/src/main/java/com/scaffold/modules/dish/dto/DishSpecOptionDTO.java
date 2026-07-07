package com.scaffold.modules.dish.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 菜品规格选项 DTO
 *
 * @author Henfon
 */
@Data
public class DishSpecOptionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 选项ID
     */
    private Long id;

    /**
     * 选项名称
     */
    @NotBlank(message = "规格选项名称不能为空")
    @Size(max = 50, message = "规格选项名称长度不能超过50")
    private String name;

    /**
     * 排序序号
     */
    private Integer sort = 0;
}
