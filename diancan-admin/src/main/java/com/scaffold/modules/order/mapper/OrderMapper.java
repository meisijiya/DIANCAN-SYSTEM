package com.scaffold.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
