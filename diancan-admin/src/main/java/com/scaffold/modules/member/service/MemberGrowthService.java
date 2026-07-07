package com.scaffold.modules.member.service;

import com.scaffold.common.result.PageResult;
import com.scaffold.modules.member.dto.MemberGrowthRecordQueryDTO;
import com.scaffold.modules.member.entity.MemberGrowthRecord;
import com.scaffold.modules.member.entity.MemberProfile;
import com.scaffold.modules.member.vo.MemberGrowthRecordVO;

import java.math.BigDecimal;

/**
 * 会员成长值服务
 *
 * @author Henfon
 */
public interface MemberGrowthService {

    PageResult<MemberGrowthRecordVO> pageAdmin(MemberGrowthRecordQueryDTO dto);

    PageResult<MemberGrowthRecordVO> pageCurrentMember(Long userId, int pageNum, int pageSize);

    boolean existsByBiz(String bizType, Long bizId);

    void addGrowthForOrder(MemberProfile profile, Long orderId, Integer growthValue, String remark);

    void insertRecord(MemberGrowthRecord record);

    void rollbackGrowthForRefund(MemberProfile profile, Long orderId, BigDecimal orderAmount, BigDecimal refundAmount, String remark);

    /**
     * 查询订单支付奖励成长值
     *
     * @param userId 用户ID
     * @param orderId 订单ID
     * @return 奖励成长值
     * @author Henfon
     * @date 2026-07-01
     * @description 用于支付成功页展示本次到账成长值
     */
    Integer getRewardGrowthByOrder(Long userId, Long orderId);
}
