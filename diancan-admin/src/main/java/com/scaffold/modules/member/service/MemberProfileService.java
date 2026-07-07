package com.scaffold.modules.member.service;

import com.scaffold.common.result.PageResult;
import com.scaffold.modules.member.dto.MemberQueryDTO;
import com.scaffold.modules.member.entity.MemberProfile;
import com.scaffold.modules.member.vo.AppMemberCenterVO;
import com.scaffold.modules.member.vo.MemberDetailVO;
import com.scaffold.modules.member.vo.MemberOverviewVO;
import com.scaffold.modules.member.vo.MemberProfileVO;

/**
 * 会员档案服务
 *
 * @author Henfon
 */
public interface MemberProfileService {

    PageResult<MemberProfileVO> pageList(MemberQueryDTO dto);

    MemberDetailVO getDetail(Long id);

    MemberProfile getOrCreateByUserId(Long userId);

    MemberProfile getByIdForUpdate(Long id);

    AppMemberCenterVO getCurrentMemberCenter(Long userId);

    /**
     * 查询会员统计总览
     *
     * @return 会员统计数据
     * @author Henfon
     * @date 2026-07-01
     * @description 汇总会员规模、积分资产、等级分布与近7天新增趋势
     */
    MemberOverviewVO getOverview();
}
