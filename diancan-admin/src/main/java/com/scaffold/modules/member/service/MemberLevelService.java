package com.scaffold.modules.member.service;

import com.scaffold.modules.member.dto.MemberLevelCreateDTO;
import com.scaffold.modules.member.dto.MemberLevelUpdateDTO;
import com.scaffold.modules.member.entity.MemberLevel;
import com.scaffold.modules.member.entity.MemberProfile;
import com.scaffold.modules.member.vo.MemberLevelVO;

import java.util.List;

/**
 * 会员等级服务
 *
 * @author Henfon
 */
public interface MemberLevelService {

    List<MemberLevelVO> listAll();

    List<MemberLevelVO> listEnabled();

    void create(MemberLevelCreateDTO dto);

    void update(MemberLevelUpdateDTO dto);

    void updateStatus(Long id, Integer status);

    MemberLevel getByIdOrThrow(Long id);

    MemberLevel getDefaultLevel();

    MemberLevel matchLevelByGrowthValue(Integer growthValue);

    void upgradeIfNeeded(MemberProfile profile, String bizType, Long bizId);
}
