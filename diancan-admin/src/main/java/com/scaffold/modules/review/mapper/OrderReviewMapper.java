package com.scaffold.modules.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.review.entity.OrderReview;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单评价 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface OrderReviewMapper extends BaseMapper<OrderReview> {
}
