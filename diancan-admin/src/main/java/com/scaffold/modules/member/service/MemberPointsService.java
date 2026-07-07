package com.scaffold.modules.member.service;

import com.scaffold.common.result.PageResult;
import com.scaffold.modules.member.dto.MemberPointsAdjustDTO;
import com.scaffold.modules.member.dto.MemberPointsRecordQueryDTO;
import com.scaffold.modules.member.entity.MemberPointsRecord;
import com.scaffold.modules.member.entity.MemberProfile;
import com.scaffold.modules.member.vo.MemberPointsRecordVO;

import java.math.BigDecimal;

/**
 * 会员积分服务
 *
 * @author Henfon
 */
public interface MemberPointsService {

    PageResult<MemberPointsRecordVO> pageAdmin(MemberPointsRecordQueryDTO dto);

    PageResult<MemberPointsRecordVO> pageCurrentMember(Long userId, int pageNum, int pageSize);

    boolean existsByBiz(String bizType, Long bizId);

    void addPointsForOrder(MemberProfile profile, Long orderId, Integer points, String remark);

    void adjustPoints(Long memberId, MemberPointsAdjustDTO dto);

    void insertRecord(MemberPointsRecord record);

    void rollbackPointsForRefund(MemberProfile profile, Long orderId, BigDecimal orderAmount, BigDecimal refundAmount, String remark);

    void deductPointsForOrder(MemberProfile profile, Long orderId, Integer points, String remark);

    void returnPointsForOrder(MemberProfile profile, Long orderId, Integer points, String remark);

    /**
     * 查询订单支付奖励积分
     *
     * @param userId 用户ID
     * @param orderId 订单ID
     * @return 奖励积分
     * @author Henfon
     * @date 2026-07-01
     * @description 用于支付成功页展示本次到账积分
     */
    Integer getRewardPointsByOrder(Long userId, Long orderId);

    /**
     * 查询订单已抵扣积分
     *
     * @param userId 用户ID
     * @param orderId 订单ID
     * @return 已抵扣积分
     * @author Henfon
     * @date 2026-07-02
     * @description 用于会员和支付结果页回显本单已使用积分
     */
    Integer getDeductedPointsByOrder(Long userId, Long orderId);
}
