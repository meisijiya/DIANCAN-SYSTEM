package com.scaffold.modules.review.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价 VO
 *
 * @author Henfon
 */
@Data
public class ReviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orderId;

    private Integer overallRating;

    private String content;

    private String customerOpenid;

    private LocalDateTime createTime;

    /**
     * 订单项评价列表
     */
    private List<ItemReviewVO> itemReviews;

    /**
     * 订单项评价 VO
     */
    @Data
    public static class ItemReviewVO implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;

        private Long orderItemId;

        private Integer rating;
    }
}
