package com.scaffold.modules.member.service;

import java.math.BigDecimal;

/**
 * 会员结算服务
 *
 * @author Henfon
 */
public interface MemberSettlementService {

    void settleAfterOrderPaid(Long orderId);

    void rollbackAfterOrderRefund(Long orderId, BigDecimal refundAmount);
}
