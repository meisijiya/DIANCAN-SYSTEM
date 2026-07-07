package com.scaffold.modules.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.order.entity.Order;
import com.scaffold.modules.order.service.OrderService;
import com.scaffold.modules.review.dto.ReviewCreateDTO;
import com.scaffold.modules.review.dto.ReviewQueryDTO;
import com.scaffold.modules.review.entity.OrderItemReview;
import com.scaffold.modules.review.entity.OrderReview;
import com.scaffold.modules.review.mapper.OrderItemReviewMapper;
import com.scaffold.modules.review.mapper.OrderReviewMapper;
import com.scaffold.modules.review.service.ReviewService;
import com.scaffold.modules.review.vo.AdminReviewListVO;
import com.scaffold.modules.review.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 评价服务实现
 *
 * @author Henfon
 */
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final OrderReviewMapper orderReviewMapper;
    private final OrderItemReviewMapper orderItemReviewMapper;
    private final OrderService orderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewVO submitReview(String openid, ReviewCreateDTO dto) {
        // 校验订单存在
        Order order = orderService.getById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }

        // 校验订单已支付（status == 1）
        if (order.getStatus() != 1) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        // 校验未重复评价
        LambdaQueryWrapper<OrderReview> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(OrderReview::getOrderId, dto.getOrderId());
        if (orderReviewMapper.selectCount(existWrapper) > 0) {
            throw new BusinessException(ResultCode.REVIEW_DUPLICATE);
        }

        // 校验总体评分范围
        if (dto.getOverallRating() < 1 || dto.getOverallRating() > 5) {
            throw new BusinessException(ResultCode.REVIEW_RATING_OUT_OF_RANGE);
        }

        // 校验订单项评分范围
        if (dto.getItemRatings() != null) {
            for (ReviewCreateDTO.ItemRatingDTO item : dto.getItemRatings()) {
                if (item.getRating() < 1 || item.getRating() > 5) {
                    throw new BusinessException(ResultCode.REVIEW_RATING_OUT_OF_RANGE);
                }
            }
        }

        // 创建订单评价
        OrderReview review = new OrderReview();
        review.setOrderId(dto.getOrderId());
        review.setOverallRating(dto.getOverallRating());
        review.setContent(dto.getContent());
        review.setCustomerOpenid(openid);
        orderReviewMapper.insert(review);

        // 创建订单项评价
        List<OrderItemReview> itemReviews = new ArrayList<>();
        if (dto.getItemRatings() != null) {
            for (ReviewCreateDTO.ItemRatingDTO itemRating : dto.getItemRatings()) {
                OrderItemReview itemReview = new OrderItemReview();
                itemReview.setReviewId(review.getId());
                itemReview.setOrderItemId(itemRating.getOrderItemId());
                itemReview.setRating(itemRating.getRating());
                orderItemReviewMapper.insert(itemReview);
                itemReviews.add(itemReview);
            }
        }

        return toVO(review, itemReviews);
    }

    @Override
    public ReviewVO getOrderReview(Long orderId) {
        LambdaQueryWrapper<OrderReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderReview::getOrderId, orderId);
        OrderReview review = orderReviewMapper.selectOne(wrapper);
        if (review == null) {
            return null;
        }

        // 查询订单项评价
        LambdaQueryWrapper<OrderItemReview> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItemReview::getReviewId, review.getId());
        List<OrderItemReview> itemReviews = orderItemReviewMapper.selectList(itemWrapper);

        return toVO(review, itemReviews);
    }

    @Override
    public PageResult<ReviewVO> listMyReviews(String openid, int pageNum, int pageSize) {
        LambdaQueryWrapper<OrderReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderReview::getCustomerOpenid, openid)
                .orderByDesc(OrderReview::getCreateTime);

        Page<OrderReview> pageResult = orderReviewMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<ReviewVO> voList = pageResult.getRecords().stream().map(review -> {
            LambdaQueryWrapper<OrderItemReview> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(OrderItemReview::getReviewId, review.getId());
            List<OrderItemReview> itemReviews = orderItemReviewMapper.selectList(itemWrapper);
            return toVO(review, itemReviews);
        }).toList();

        return PageResult.of(voList, pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
    }

    @Override
    public PageResult<AdminReviewListVO> listReviewsForAdmin(int pageNum, int pageSize, ReviewQueryDTO queryDTO) {
        LambdaQueryWrapper<OrderReview> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO != null) {
            wrapper.eq(queryDTO.getOrderId() != null, OrderReview::getOrderId, queryDTO.getOrderId())
                    .eq(queryDTO.getOverallRating() != null, OrderReview::getOverallRating, queryDTO.getOverallRating())
                    .like(queryDTO.getCustomerOpenid() != null && !queryDTO.getCustomerOpenid().isBlank(),
                            OrderReview::getCustomerOpenid, queryDTO.getCustomerOpenid());
            if (queryDTO.getStartDate() != null) {
                wrapper.ge(OrderReview::getCreateTime, queryDTO.getStartDate().atStartOfDay());
            }
            if (queryDTO.getEndDate() != null) {
                wrapper.le(OrderReview::getCreateTime, queryDTO.getEndDate().plusDays(1).atStartOfDay());
            }
        }
        wrapper.orderByDesc(OrderReview::getCreateTime);

        Page<OrderReview> pageResult = orderReviewMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<AdminReviewListVO> voList = pageResult.getRecords().stream().map(review -> {
            AdminReviewListVO vo = new AdminReviewListVO();
            vo.setId(review.getId());
            vo.setOrderId(review.getOrderId());
            vo.setOverallRating(review.getOverallRating());
            vo.setContent(review.getContent());
            vo.setCustomerOpenid(review.getCustomerOpenid());
            vo.setCreateTime(review.getCreateTime());
            Order order = orderService.getById(review.getOrderId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
                vo.setTableCode(order.getTableCode());
            }
            return vo;
        }).toList();

        return PageResult.of(voList, pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
    }

    private ReviewVO toVO(OrderReview review, List<OrderItemReview> itemReviews) {
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setOrderId(review.getOrderId());
        vo.setOverallRating(review.getOverallRating());
        vo.setContent(review.getContent());
        vo.setCustomerOpenid(review.getCustomerOpenid());
        vo.setCreateTime(review.getCreateTime());

        if (itemReviews != null && !itemReviews.isEmpty()) {
            List<ReviewVO.ItemReviewVO> itemVOs = itemReviews.stream().map(item -> {
                ReviewVO.ItemReviewVO itemVO = new ReviewVO.ItemReviewVO();
                itemVO.setId(item.getId());
                itemVO.setOrderItemId(item.getOrderItemId());
                itemVO.setRating(item.getRating());
                return itemVO;
            }).toList();
            vo.setItemReviews(itemVOs);
        } else {
            vo.setItemReviews(Collections.emptyList());
        }

        return vo;
    }
}
