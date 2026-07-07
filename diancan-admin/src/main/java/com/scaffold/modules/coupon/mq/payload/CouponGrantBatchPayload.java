package com.scaffold.modules.coupon.mq.payload;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 发券批处理消息体
 *
 * @author Henfon
 */
@Data
public class CouponGrantBatchPayload implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 发券任务ID
     */
    private Long taskId;

    /**
     * 批次序号
     */
    private Integer batchNo;

    /**
     * 任务用户ID列表
     */
    private List<Long> taskUserIds;
}
