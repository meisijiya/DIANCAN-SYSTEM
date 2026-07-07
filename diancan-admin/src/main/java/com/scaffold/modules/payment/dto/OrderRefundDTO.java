package com.scaffold.modules.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 整单退款参数
 *
 * @author Henfon
 */
@Data
@Schema(description = "整单退款参数")
public class OrderRefundDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "退款原因不能为空")
    @Schema(description = "退款原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}
