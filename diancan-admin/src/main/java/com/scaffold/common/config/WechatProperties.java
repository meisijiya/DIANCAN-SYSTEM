package com.scaffold.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信小程序配置属性
 *
 * @author Henfon
 * @date 2025/06/25
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wechat.miniapp")
public class WechatProperties {

    /** 小程序 appId */
    private String appId;

    /** 小程序 appSecret */
    private String appSecret;
}
