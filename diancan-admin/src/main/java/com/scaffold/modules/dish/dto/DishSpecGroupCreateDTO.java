package com.scaffold.modules.dish.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜品规格组创建 DTO
 *
 * @author Henfon
 */
@Data
public class DishSpecGroupCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规格组名称
     */
    @NotBlank(message = "规格组名称不能为空")
    @Size(max = 50, message = "规格组名称长度不能超过50")
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
     * 规格选项
     */
    @Valid
    private List<DishSpecOptionDTO> options = new ArrayList<>();
}
