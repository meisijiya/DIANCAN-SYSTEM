package com.scaffold.modules.feedback.service;

import com.scaffold.common.result.PageResult;
import com.scaffold.modules.feedback.dto.FeedbackCreateDTO;
import com.scaffold.modules.feedback.dto.FeedbackQueryDTO;
import com.scaffold.modules.feedback.dto.FeedbackReplyDTO;
import com.scaffold.modules.feedback.vo.AdminFeedbackListVO;
import com.scaffold.modules.feedback.vo.FeedbackVO;

/**
 * 反馈服务接口
 *
 * @author Henfon
 */
public interface FeedbackService {

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：提交用户反馈
     *
     * @param openid 当前用户 openid
     * @param dto 反馈参数
     * @return 反馈信息
     */
    FeedbackVO submitFeedback(String openid, FeedbackCreateDTO dto);

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：分页查询我的反馈
     *
     * @param openid 当前用户 openid
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 反馈分页列表
     */
    PageResult<FeedbackVO> listMyFeedback(String openid, int pageNum, int pageSize);

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：分页查询管理端反馈列表
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param queryDTO 查询参数
     * @return 反馈分页列表
     */
    PageResult<AdminFeedbackListVO> listFeedbackForAdmin(int pageNum, int pageSize, FeedbackQueryDTO queryDTO);

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：回复用户反馈
     *
     * @param feedbackId 反馈ID
     * @param dto 回复参数
     */
    void replyFeedback(Long feedbackId, FeedbackReplyDTO dto);
}
