package com.scaffold.modules.payment.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.modules.payment.entity.PaymentRecord;
import com.scaffold.modules.payment.service.PaymentService;
import com.scaffold.modules.payment.vo.PaymentStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付对账定时任务
 * 定期查询待支付状态的支付记录，对超时未支付的记录进行处理：
 * - 超过5分钟未收到回调的支付记录标记为支付失败(status=3)
 * - 实际生产环境应主动查询第三方支付平台获取真实支付结果
 *
 * @author Henfon
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationTask {

    private final PaymentService paymentService;

    /**
     * 每30秒执行一次支付对账
     * 查询创建时间超过30秒且状态仍为待支付(0)的记录
     * 超过5分钟的记录标记为支付失败(3)
     */
    @Scheduled(fixedDelay = 30000)
    public void reconcilePendingPayments() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);

        // 查询待支付且创建时间超过30秒的记录
        List<PaymentRecord> pendingRecords = paymentService.list(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getStatus, 0)
                        .lt(PaymentRecord::getCreateTime, threshold)
        );

        if (pendingRecords.isEmpty()) {
            return;
        }

        log.info("支付对账任务：发现 {} 条待确认支付记录", pendingRecords.size());

        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(5);

        for (PaymentRecord record : pendingRecords) {
            try {
                log.info("支付对账：主动查询支付记录 paymentNo={}, paymentMethod={}, createTime={}",
                        record.getPaymentNo(), record.getPaymentMethod(), record.getCreateTime());

                // 复用支付状态查询逻辑，内部会对接第三方查询并在已支付时自动落账
                PaymentStatusVO statusVO = paymentService.getPaymentStatus(record.getId());
                if (statusVO != null && statusVO.getStatus() != null && statusVO.getStatus() == 1) {
                    log.info("支付对账：支付已确认 paymentNo={}", record.getPaymentNo());
                    continue;
                }

                // 超过5分钟未支付，标记为支付失败
                if (record.getCreateTime() != null && record.getCreateTime().isBefore(timeoutThreshold)) {
                    record.setStatus(3); // 支付失败
                    paymentService.updateById(record);
                    log.warn("支付对账：支付记录超时标记为失败 paymentNo={}, orderId={}",
                            record.getPaymentNo(), record.getOrderId());
                }
            } catch (Exception e) {
                log.error("支付对账：处理支付记录异常 paymentNo={}, error={}",
                        record.getPaymentNo(), e.getMessage(), e);
            }
        }
    }
}
