package com.scaffold.modules.order.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * 订单详情 VO（管理端，含操作日志）
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDetailVO extends OrderVO {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 操作日志列表
     */
    private List<OrderOperationLogVO> operationLogs;
}
