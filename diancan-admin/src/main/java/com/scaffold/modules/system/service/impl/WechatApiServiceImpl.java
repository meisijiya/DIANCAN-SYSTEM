package com.scaffold.modules.system.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.scaffold.common.config.WechatProperties;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.ResultCode;
import com.scaffold.framework.redis.RedisUtils;
import com.scaffold.modules.system.service.WechatApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 微信小程序 API 服务实现
 *
 * @author Henfon
 * @date 2025/06/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatApiServiceImpl implements WechatApiService {

    private static final String ACCESS_TOKEN_KEY = "wechat:miniapp:access-token";
    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";
    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String GET_PHONE_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";

    private final WechatProperties wechatProperties;
    private final RedisUtils redisUtils;

    @Override
    public WechatSessionInfo code2Session(String code) {
        String appId = wechatProperties.getAppId();
        String secret = wechatProperties.getAppSecret();

        try (HttpResponse response = HttpRequest.get(CODE2SESSION_URL)
                .form("appid", appId)
                .form("secret", secret)
                .form("js_code", code)
                .form("grant_type", "authorization_code")
                .execute()) {

            JSONObject json = JSONUtil.parseObj(response.body());
            // errcode=0 表示成功，不返回 errcode 或 errcode=0
            int errcode = json.getInt("errcode", 0);
            if (errcode != 0) {
                String errmsg = json.getStr("errmsg", "");
                log.error("code2Session 失败: errcode={}, errmsg={}, appId={}", errcode, errmsg, appId);
                String hint = errcode == 40029 ? "code无效或已过期" :
                              errcode == 40125 ? "appSecret配置错误" :
                              errcode == -1 ? "系统繁忙，请稍后重试" : "请重试";
                throw new BusinessException(ResultCode.LOGIN_ERROR, "微信登录失败: " + hint + " [" + errcode + "]");
            }

            String openid = json.getStr("openid");
            String sessionKey = json.getStr("session_key");
            if (!StringUtils.hasText(openid) || !StringUtils.hasText(sessionKey)) {
                throw new BusinessException(ResultCode.LOGIN_ERROR, "微信登录失败，请重试");
            }

            return new WechatSessionInfo(openid, sessionKey);
        }
    }

    @Override
    public String getPhoneNumber(String phoneCode) {
        String accessToken = fetchAccessToken();
        JSONObject body = new JSONObject();
        body.set("code", phoneCode);

        try (HttpResponse response = HttpRequest.post(GET_PHONE_URL + "?access_token=" + accessToken)
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(body.toString())
                .execute()) {

            JSONObject json = JSONUtil.parseObj(response.body());
            int errcode = json.getInt("errcode", 0);
            if (errcode != 0) {
                String errmsg = json.getStr("errmsg", "");
                log.error("获取手机号失败: errcode={}, errmsg={}, phoneCode长度={}", errcode, errmsg,
                        phoneCode != null ? phoneCode.length() : 0);
                String hint = errcode == 40029 ? "phoneCode无效" :
                              errcode == 40001 ? "access_token无效或过期" :
                              errcode == 48001 ? "未获得API权限，请检查小程序认证状态" : "请重新授权";
                throw new BusinessException(ResultCode.LOGIN_ERROR, "获取手机号失败: " + hint + " [" + errcode + "]");
            }

            JSONObject phoneInfo = json.getJSONObject("phone_info");
            if (phoneInfo == null) {
                throw new BusinessException(ResultCode.LOGIN_ERROR, "获取手机号失败，请重试");
            }

            // phone_info 里的 phoneNumber 包含国家代码（如 +8613800138000），提取纯手机号
            String phoneNumber = phoneInfo.getStr("phoneNumber");
            if (!StringUtils.hasText(phoneNumber)) {
                throw new BusinessException(ResultCode.LOGIN_ERROR, "获取手机号为空，请重试");
            }

            // 去掉国家代码前缀
            String purePhone = phoneInfo.getStr("purePhoneNumber");
            return StringUtils.hasText(purePhone) ? purePhone : phoneNumber.replaceAll("[^0-9]", "");
        }
    }

    /**
     * 获取微信 access_token（Redis 缓存）
     */
    private String fetchAccessToken() {
        Object cached = redisUtils.get(ACCESS_TOKEN_KEY);
        if (cached instanceof String cachedToken && StringUtils.hasText(cachedToken)) {
            return cachedToken;
        }

        try (HttpResponse response = HttpRequest.get(TOKEN_URL)
                .form("grant_type", "client_credential")
                .form("appid", wechatProperties.getAppId())
                .form("secret", wechatProperties.getAppSecret())
                .execute()) {

            JSONObject json = JSONUtil.parseObj(response.body());
            String token = json.getStr("access_token");
            Integer expiresIn = json.getInt("expires_in", 7200);
            if (!StringUtils.hasText(token)) {
                Integer errCode = json.getInt("errcode", -1);
                String errMsg = json.getStr("errmsg", "unknown");
                log.error("获取微信 access_token 失败: errcode={}, errmsg={}, appId={}", errCode, errMsg,
                        wechatProperties.getAppId());
                throw new BusinessException(ResultCode.FAIL,
                        "获取微信 access_token 失败: " + errCode + " " + errMsg);
            }

            long cacheSeconds = Math.max(300, expiresIn - 120L);
            redisUtils.set(ACCESS_TOKEN_KEY, token, cacheSeconds);
            return token;
        }
    }
}
