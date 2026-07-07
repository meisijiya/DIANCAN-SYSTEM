package com.scaffold.modules.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 订单操作日志实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_operation_log")
public class OrderOperationLog extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 关联订单项ID（可空）
     */
    private Long orderItemId;

    /**
     * 操作类型（RETURN/REPLACE/GIFT/DISCOUNT/RUSH）
     */
    private String operationType;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作原因
     */
    private String reason;

    /**
     * 操作详情（JSON）
     */
    private String detail;
}
