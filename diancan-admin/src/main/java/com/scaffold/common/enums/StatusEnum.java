package com.scaffold.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 状态枚举
 *
 * @author Henfon
 */
@Getter
@AllArgsConstructor
public enum StatusEnum {

    /**
     * 禁用
     */
    DISABLED(0, "禁用"),

    /**
     * 启用
     */
    ENABLED(1, "启用");

    /**
     * 状态值
     */
    private final Integer value;

    /**
     * 描述
     */
    private final String desc;

    /**
     * 根据值获取枚举
     */
    public static StatusEnum of(Integer value) {
        for (StatusEnum status : values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否启用
     */
    public static boolean isEnabled(Integer value) {
        return ENABLED.getValue().equals(value);
    }
}
