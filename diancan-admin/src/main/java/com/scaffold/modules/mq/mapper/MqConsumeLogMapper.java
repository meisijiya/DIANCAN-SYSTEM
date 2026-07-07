package com.scaffold.modules.mq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.mq.entity.MqConsumeLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * MQ 消费日志 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface MqConsumeLogMapper extends BaseMapper<MqConsumeLog> {
}
