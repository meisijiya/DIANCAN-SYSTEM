package com.scaffold.modules.system.service;

/**
 * 微信小程序 API 服务接口
 *
 * @author Henfon
 * @date 2025/06/25
 */
public interface WechatApiService {

    /**
     * 用微信临时凭证 code 换取 openid 和 session_key
     *
     * @param code 微信 wx.login 返回的临时凭证
     * @return {openid, sessionKey}
     */
    WechatSessionInfo code2Session(String code);

    /**
     * 用 phoneCode 获取用户手机号
     *
     * @param phoneCode 微信 getPhoneNumber 返回的 code
     * @return 手机号（不含国家代码前缀）
     */
    String getPhoneNumber(String phoneCode);

    /**
     * code2Session 返回信息
     */
    record WechatSessionInfo(String openid, String sessionKey) {}
}
