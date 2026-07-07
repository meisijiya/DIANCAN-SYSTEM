package com.scaffold.modules.report.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 首页经营提醒 VO
 *
 * @author Henfon
 */
@Data
public class DashboardAlertVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 提醒标题
     */
    private String title;

    /**
     * 提醒内容
     */
    private String detail;

    /**
     * 提醒级别：danger/warning/neutral
     */
    private String tone;

    /**
     * 操作文案
     */
    private String actionLabel;

    /**
     * 跳转路由
     */
    private String actionTo;
}
