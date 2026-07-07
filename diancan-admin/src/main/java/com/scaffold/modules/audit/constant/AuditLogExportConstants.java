package com.scaffold.modules.audit.constant;

/**
 * 审计日志导出任务常量
 *
 * @author Henfon
 */
public final class AuditLogExportConstants {

    /**
     * 待处理
     */
    public static final Integer TASK_PENDING = 0;

    /**
     * 处理中
     */
    public static final Integer TASK_PROCESSING = 1;

    /**
     * 成功
     */
    public static final Integer TASK_SUCCESS = 2;

    /**
     * 失败
     */
    public static final Integer TASK_FAILED = 3;

    private AuditLogExportConstants() {
    }
}
