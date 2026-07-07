package com.scaffold.framework.satoken;

import cn.dev33.satoken.stp.StpUtil;

/**
 * Sa-Token Session 工具类
 *
 * @author Henfon
 * @date 2025/06/25
 */
public final class SessionUtils {

    private SessionUtils() {}

    /**
     * 从当前 Sa-Token Session 中获取小程序用户的 openid
     *
     * @return openid，未登录时返回空字符串
     */
    public static String getCurrentOpenid() {
        Object openid = StpUtil.getSession().get("openid");
        return openid != null ? openid.toString() : "";
    }
}
