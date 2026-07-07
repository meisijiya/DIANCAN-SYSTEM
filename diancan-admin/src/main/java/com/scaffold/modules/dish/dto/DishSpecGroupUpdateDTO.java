package com.scaffold.modules.dish.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 菜品规格组更新 DTO
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DishSpecGroupUpdateDTO extends DishSpecGroupCreateDTO {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规格组ID
     */
    @NotNull(message = "规格组ID不能为空")
    private Long id;
}
