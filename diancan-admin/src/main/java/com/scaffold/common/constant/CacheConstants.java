package com.scaffold.common.constant;

/**
 * 缓存 Key 常量
 *
 * @author Henfon
 */
public class CacheConstants {

    /**
     * 缓存前缀
     */
    public static final String CACHE_PREFIX = "scaffold:";

    /**
     * 用户权限缓存前缀
     */
    public static final String USER_PERMISSION_KEY = CACHE_PREFIX + "user:permission:";

    /**
     * 用户角色缓存前缀
     */
    public static final String USER_ROLE_KEY = CACHE_PREFIX + "user:role:";

    /**
     * 字典缓存前缀
     */
    public static final String DICT_KEY = CACHE_PREFIX + "dict:";

    /**
     * 配置缓存前缀
     */
    public static final String CONFIG_KEY = CACHE_PREFIX + "config:";

    /**
     * 缓存过期时间（秒）- 1小时
     */
    public static final long CACHE_EXPIRE = 3600L;

    /**
     * 缓存过期时间（秒）- 1天
     */
    public static final long CACHE_EXPIRE_DAY = 86400L;

    private CacheConstants() {
    }
}
