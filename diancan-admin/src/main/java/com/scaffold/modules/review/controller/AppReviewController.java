package com.scaffold.modules.review.controller;

import com.scaffold.framework.satoken.SessionUtils;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.review.dto.ReviewCreateDTO;
import com.scaffold.modules.review.service.ReviewService;
import com.scaffold.modules.review.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 评价控制器（小程序端）
 *
 * @author Henfon
 */
@Tag(name = "评价（小程序端）")
@RestController
@RequestMapping("/app/review")
@RequiredArgsConstructor
public class AppReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "提交评价")
    @PostMapping
    public Result<ReviewVO> submitReview(@Valid @RequestBody ReviewCreateDTO dto) {
        String openid = SessionUtils.getCurrentOpenid();
        return Result.success(reviewService.submitReview(openid, dto));
    }

    @Operation(summary = "查询订单评价")
    @GetMapping("/order/{orderId}")
    public Result<ReviewVO> getOrderReview(@PathVariable Long orderId) {
        return Result.success(reviewService.getOrderReview(orderId));
    }

    @Operation(summary = "分页查询我的评价")
    @GetMapping("/my")
    public Result<PageResult<ReviewVO>> listMyReviews(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        String openid = SessionUtils.getCurrentOpenid();
        return Result.success(reviewService.listMyReviews(openid, pageNum, pageSize));
    }
}
