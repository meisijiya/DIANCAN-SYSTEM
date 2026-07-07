package com.scaffold.modules.review.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.review.dto.ReviewQueryDTO;
import com.scaffold.modules.review.service.ReviewService;
import com.scaffold.modules.review.vo.AdminReviewListVO;
import com.scaffold.modules.review.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 评价控制器（管理端）
 *
 * @author Henfon
 */
@Tag(name = "评价（管理端）")
@RestController
@RequestMapping("/admin/review")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "分页查询评价列表")
    @SaCheckPermission("review:list")
    @GetMapping("/list")
    public Result<PageResult<AdminReviewListVO>> listReviews(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize,
            ReviewQueryDTO queryDTO) {
        return Result.success(reviewService.listReviewsForAdmin(pageNum, pageSize, queryDTO));
    }

    @Operation(summary = "获取订单评价")
    @GetMapping("/order/{orderId}")
    public Result<ReviewVO> getOrderReview(@PathVariable Long orderId) {
        return Result.success(reviewService.getOrderReview(orderId));
    }
}
