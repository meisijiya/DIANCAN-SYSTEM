package com.scaffold.modules.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 退菜请求 DTO
 *
 * @author Henfon
 */
@Data
@Schema(description = "退菜请求参数")
public class ReturnItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "授权密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "授权密码不能为空")
    private String authPassword;

    @Schema(description = "退菜原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "退菜原因不能为空")
    private String reason;
}
