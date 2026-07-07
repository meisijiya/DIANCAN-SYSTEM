package com.scaffold.modules.review.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 订单项评价实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_item_review")
public class OrderItemReview extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联评价ID
     */
    private Long reviewId;

    /**
     * 关联订单项ID
     */
    private Long orderItemId;

    /**
     * 评分（1-5）
     */
    private Integer rating;
}
