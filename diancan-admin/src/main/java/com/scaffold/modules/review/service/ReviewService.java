package com.scaffold.modules.review.service;

import com.scaffold.common.result.PageResult;
import com.scaffold.modules.review.dto.ReviewCreateDTO;
import com.scaffold.modules.review.dto.ReviewQueryDTO;
import com.scaffold.modules.review.vo.AdminReviewListVO;
import com.scaffold.modules.review.vo.ReviewVO;

/**
 * 评价服务接口
 *
 * @author Henfon
 */
public interface ReviewService {

    /**
     * 提交评价（校验订单已支付、未重复评价、评分1-5范围）
     *
     * @param openid 用户openid
     * @param dto    评价参数
     * @return 评价信息
     */
    ReviewVO submitReview(String openid, ReviewCreateDTO dto);

    /**
     * 查询订单评价
     *
     * @param orderId 订单ID
     * @return 评价信息，不存在返回null
     */
    ReviewVO getOrderReview(Long orderId);

    /**
     * 小程序端分页查询我的评价
     *
     * @param openid 用户openid
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 评价分页列表
     */
    PageResult<ReviewVO> listMyReviews(String openid, int pageNum, int pageSize);

    /**
     * 管理端分页查询评价
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param queryDTO 查询条件
     * @return 评价分页列表
     */
    PageResult<AdminReviewListVO> listReviewsForAdmin(int pageNum, int pageSize, ReviewQueryDTO queryDTO);
}
