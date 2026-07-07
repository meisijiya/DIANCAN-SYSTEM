package com.scaffold.modules.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 小程序反馈提交 DTO
 *
 * @author Henfon
 */
@Data
@Schema(description = "反馈提交参数")
public class FeedbackCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "反馈内容")
    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 500, message = "反馈内容不能超过500字")
    private String content;

    @Schema(description = "联系手机号")
    @Size(max = 30, message = "联系手机号长度不能超过30位")
    private String contactPhone;
}
