package com.scaffold.modules.feedback.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.feedback.dto.FeedbackQueryDTO;
import com.scaffold.modules.feedback.dto.FeedbackReplyDTO;
import com.scaffold.modules.feedback.service.FeedbackService;
import com.scaffold.modules.feedback.vo.AdminFeedbackListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 反馈控制器（管理端）
 *
 * @author Henfon
 */
@Tag(name = "反馈（管理端）")
@RestController
@RequestMapping("/admin/feedback")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：分页查询反馈列表
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param queryDTO 查询参数
     * @return 反馈分页列表
     */
    @Operation(summary = "分页查询反馈列表")
    @SaCheckPermission("feedback:list")
    @GetMapping("/list")
    public Result<PageResult<AdminFeedbackListVO>> listFeedback(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize,
            FeedbackQueryDTO queryDTO) {
        return Result.success(feedbackService.listFeedbackForAdmin(pageNum, pageSize, queryDTO));
    }

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：回复用户反馈
     *
     * @param feedbackId 反馈ID
     * @param dto 回复参数
     * @return 操作结果
     */
    @Operation(summary = "回复用户反馈")
    @SaCheckPermission("feedback:reply")
    @PutMapping("/{feedbackId}/reply")
    public Result<Void> replyFeedback(@PathVariable Long feedbackId, @Valid @RequestBody FeedbackReplyDTO dto) {
        feedbackService.replyFeedback(feedbackId, dto);
        return Result.success();
    }
}
