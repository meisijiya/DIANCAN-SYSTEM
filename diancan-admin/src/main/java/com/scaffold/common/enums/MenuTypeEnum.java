package com.scaffold.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 菜单类型枚举
 *
 * @author Henfon
 */
@Getter
@AllArgsConstructor
public enum MenuTypeEnum {

    /**
     * 目录
     */
    DIRECTORY(0, "目录"),

    /**
     * 菜单
     */
    MENU(1, "菜单"),

    /**
     * 按钮/权限
     */
    BUTTON(2, "按钮");

    /**
     * 类型值
     */
    private final Integer value;

    /**
     * 描述
     */
    private final String desc;

    /**
     * 根据值获取枚举
     */
    public static MenuTypeEnum of(Integer value) {
        for (MenuTypeEnum type : values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
