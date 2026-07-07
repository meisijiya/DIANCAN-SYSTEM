package com.scaffold.modules.dish.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜品规格项 DTO
 *
 * @author Henfon
 */
@Data
public class DishSpecItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规格组ID
     */
    @NotNull(message = "规格组ID不能为空")
    private Long specGroupId;

    /**
     * 规格值ID列表
     */
    private List<Long> optionIds = new ArrayList<>();
}
