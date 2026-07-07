package com.scaffold.modules.feedback.controller;

import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.framework.satoken.SessionUtils;
import com.scaffold.modules.feedback.dto.FeedbackCreateDTO;
import com.scaffold.modules.feedback.service.FeedbackService;
import com.scaffold.modules.feedback.vo.FeedbackVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 反馈控制器（小程序端）
 *
 * @author Henfon
 */
@Tag(name = "反馈（小程序端）")
@RestController
@RequestMapping("/app/feedback")
@RequiredArgsConstructor
public class AppFeedbackController {

    private final FeedbackService feedbackService;

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：提交反馈
     *
     * @param dto 反馈参数
     * @return 反馈信息
     */
    @Operation(summary = "提交反馈")
    @PostMapping
    public Result<FeedbackVO> submitFeedback(@Valid @RequestBody FeedbackCreateDTO dto) {
        String openid = SessionUtils.getCurrentOpenid();
        return Result.success(feedbackService.submitFeedback(openid, dto));
    }

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：分页查询我的反馈
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 反馈分页列表
     */
    @Operation(summary = "分页查询我的反馈")
    @GetMapping("/my")
    public Result<PageResult<FeedbackVO>> listMyFeedback(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        String openid = SessionUtils.getCurrentOpenid();
        return Result.success(feedbackService.listMyFeedback(openid, pageNum, pageSize));
    }
}
