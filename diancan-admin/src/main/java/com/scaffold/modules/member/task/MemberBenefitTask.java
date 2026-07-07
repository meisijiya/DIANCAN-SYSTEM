package com.scaffold.modules.member.task;

import com.scaffold.modules.member.service.MemberBenefitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 会员权益定时任务
 *
 * @author Henfon
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberBenefitTask {

    private final MemberBenefitService memberBenefitService;

    /**
     * 每天早上发放生日权益
     *
     * @author Henfon
     * @date 2026-07-02
     * @description 扫描当天生日会员并自动补发生日券
     */
    @Scheduled(cron = "0 10 3 * * ?")
    public void grantBirthdayBenefits() {
        try {
            memberBenefitService.grantBirthdayBenefits();
        } catch (Exception e) {
            log.error("生日权益发放任务执行失败", e);
        }
    }

    /**
     * 每小时执行一次积分过期
     *
     * @author Henfon
     * @date 2026-07-02
     * @description 扫描已到期的积分批次并写入过期流水
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void expireMemberPoints() {
        try {
            memberBenefitService.expireMemberPoints();
        } catch (Exception e) {
            log.error("积分过期任务执行失败", e);
        }
    }
}
