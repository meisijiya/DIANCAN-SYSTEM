package com.scaffold.modules.order.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单操作日志 VO
 *
 * @author Henfon
 */
@Data
public class OrderOperationLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orderId;

    private Long orderItemId;

    private String operationType;

    private Long operatorId;

    private String operatorName;

    private String reason;

    private String detail;

    private LocalDateTime createTime;
}
