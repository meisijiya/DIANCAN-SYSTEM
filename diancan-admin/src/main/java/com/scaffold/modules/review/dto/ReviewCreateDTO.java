package com.scaffold.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 提交评价请求 DTO
 *
 * @author Henfon
 */
@Data
@Schema(description = "提交评价请求参数")
public class ReviewCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "总体评分（1-5）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "总体评分不能为空")
    @Min(value = 1, message = "评分最低为1")
    @Max(value = 5, message = "评分最高为5")
    private Integer overallRating;

    @Schema(description = "文字评价")
    private String content;

    @Schema(description = "订单项评分列表")
    @Valid
    private List<ItemRatingDTO> itemRatings;

    /**
     * 订单项评分 DTO
     */
    @Data
    @Schema(description = "订单项评分")
    public static class ItemRatingDTO implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "订单项ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "订单项ID不能为空")
        private Long orderItemId;

        @Schema(description = "评分（1-5）", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "评分不能为空")
        @Min(value = 1, message = "评分最低为1")
        @Max(value = 5, message = "评分最高为5")
        private Integer rating;
    }
}
