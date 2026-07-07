package com.scaffold.modules.dish.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 菜品分类排序 DTO
 *
 * @author Henfon
 */
@Data
public class DishCategorySortDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 排序项列表
     */
    @NotEmpty(message = "排序列表不能为空")
    @Valid
    private List<SortItem> items;

    /**
     * 排序项
     */
    @Data
    public static class SortItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 分类ID
         */
        @NotNull(message = "分类ID不能为空")
        private Long id;

        /**
         * 排序序号
         */
        @NotNull(message = "排序序号不能为空")
        private Integer sort;
    }
}
