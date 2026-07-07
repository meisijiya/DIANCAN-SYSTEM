package com.scaffold.modules.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 加菜请求 DTO
 *
 * @author Henfon
 */
@Data
@Schema(description = "加菜请求参数")
public class AddItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "菜品ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "菜品ID不能为空")
    private Long dishId;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    @Schema(description = "口味备注")
    private String remark;
}
