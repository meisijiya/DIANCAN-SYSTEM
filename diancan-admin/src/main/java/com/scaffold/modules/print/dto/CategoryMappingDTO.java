package com.scaffold.modules.print.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分类映射批量更新 DTO
 *
 * @author Henfon
 */
@Data
public class CategoryMappingDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 映射列表
     */
    @NotNull(message = "映射列表不能为空")
    @Valid
    private List<CategoryMappingItemDTO> mappings;

    /**
     * 需要清理映射的打印机ID列表（用于“清空该打印机所有分类映射”场景）
     */
    private List<Long> printerIds;
}
