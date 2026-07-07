package com.scaffold.modules.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 换菜请求 DTO
 *
 * @author Henfon
 */
@Data
@Schema(description = "换菜请求参数")
public class ReplaceItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "新菜品ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "新菜品ID不能为空")
    private Long newDishId;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    @Schema(description = "口味备注")
    private String remark;

    @Schema(description = "授权密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "授权密码不能为空")
    private String authPassword;

    @Schema(description = "换菜原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "换菜原因不能为空")
    private String reason;
}
