package com.scaffold.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.order.entity.OrderOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单操作日志 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface OrderOperationLogMapper extends BaseMapper<OrderOperationLog> {
}
