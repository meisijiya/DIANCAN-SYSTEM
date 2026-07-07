package com.scaffold.modules.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.member.entity.MemberBenefitGrantLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员权益发放日志 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface MemberBenefitGrantLogMapper extends BaseMapper<MemberBenefitGrantLog> {
}
