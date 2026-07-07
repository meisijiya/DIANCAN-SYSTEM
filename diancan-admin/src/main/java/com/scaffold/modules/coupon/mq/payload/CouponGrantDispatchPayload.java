package com.scaffold.modules.coupon.mq.payload;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 发券任务分发消息体
 *
 * @author Henfon
 */
@Data
public class CouponGrantDispatchPayload implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 发券任务ID
     */
    private Long taskId;

    /**
     * 发放方式（1指定用户 2全部用户）
     */
    private Integer grantMode;

    /**
     * 指定用户ID列表
     */
    private List<Long> userIds;
}
