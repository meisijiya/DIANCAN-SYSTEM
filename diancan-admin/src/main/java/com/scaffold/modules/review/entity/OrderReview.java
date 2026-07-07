package com.scaffold.modules.review.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 订单评价实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_review")
public class OrderReview extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 总体评分（1-5）
     */
    private Integer overallRating;

    /**
     * 文字评价
     */
    private String content;

    /**
     * 评价人openid
     */
    private String customerOpenid;
}
