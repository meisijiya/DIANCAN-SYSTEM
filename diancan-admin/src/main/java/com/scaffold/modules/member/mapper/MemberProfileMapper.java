package com.scaffold.modules.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.member.entity.MemberProfile;
import org.apache.ibatis.annotations.Param;

/**
 * 会员档案 Mapper
 *
 * @author Henfon
 */
public interface MemberProfileMapper extends BaseMapper<MemberProfile> {

    MemberProfile selectByIdForUpdate(@Param("id") Long id);

    MemberProfile selectByUserIdForUpdate(@Param("userId") Long userId);
}
