package com.scaffold.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单项 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
