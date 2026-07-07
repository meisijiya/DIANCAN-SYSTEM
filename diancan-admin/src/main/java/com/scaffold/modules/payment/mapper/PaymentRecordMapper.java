package com.scaffold.modules.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.payment.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付记录 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}
