package com.scaffold.modules.member.service;

import com.scaffold.modules.member.dto.MemberBenefitConfigSaveDTO;
import com.scaffold.modules.member.dto.MemberCouponExchangeSaveDTO;
import com.scaffold.modules.member.vo.AppMemberBenefitOverviewVO;
import com.scaffold.modules.member.vo.MemberBenefitConfigVO;
import com.scaffold.modules.member.vo.MemberCouponExchangeVO;
import com.scaffold.modules.member.vo.MemberPointsDeductionPreviewVO;
import com.scaffold.modules.order.entity.Order;

import java.math.BigDecimal;
import java.util.List;

/**
 * 会员权益服务
 *
 * @author Henfon
 */
public interface MemberBenefitService {

    MemberBenefitConfigVO getBenefitConfig();

    void saveBenefitConfig(MemberBenefitConfigSaveDTO dto);

    List<MemberCouponExchangeVO> listExchangeConfigs();

    void saveExchangeConfig(MemberCouponExchangeSaveDTO dto);

    void deleteExchangeConfig(Long id);

    MemberPointsDeductionPreviewVO previewPointsDeduction(Long userId, BigDecimal orderAmount, Integer requestedPoints);

    AppMemberBenefitOverviewVO getAppBenefitOverview(Long userId);

    void exchangeCoupon(Long userId, Long exchangeConfigId);

    void claimExclusiveCoupon(Long userId);

    void grantBirthdayBenefits();

    void expireMemberPoints();

    void grantUpgradeGift(Long memberId, Long levelId);

    void adjustOrderPointsDeduction(Order order, Long userId);
}
