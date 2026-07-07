package com.scaffold.modules.member.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 会员奖励配置
 *
 * @author Henfon
 */
@Data
@Component
@ConfigurationProperties(prefix = "member.reward")
public class MemberRewardProperties {

    /**
     * 每消费1元对应的基础积分
     */
    private Integer pointsPerYuan = 1;

    /**
     * 每消费1元对应的基础成长值
     */
    private Integer growthPerYuan = 1;
}
