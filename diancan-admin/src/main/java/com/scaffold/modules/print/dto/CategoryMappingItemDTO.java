package com.scaffold.modules.print.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分类映射项 DTO
 *
 * @author Henfon
 */
@Data
public class CategoryMappingItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 打印机ID
     */
    @NotNull(message = "打印机ID不能为空")
    private Long printerId;

    /**
     * 菜品分类ID
     */
    @NotNull(message = "菜品分类ID不能为空")
    private Long categoryId;
}
