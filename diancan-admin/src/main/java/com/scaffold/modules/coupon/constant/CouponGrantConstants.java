package com.scaffold.modules.coupon.constant;

/**
 * 发券任务常量
 *
 * @author Henfon
 */
public final class CouponGrantConstants {

    /**
     * 任务待处理
     */
    public static final int TASK_PENDING = 0;

    /**
     * 任务分发中
     */
    public static final int TASK_DISPATCHING = 1;

    /**
     * 任务处理中
     */
    public static final int TASK_PROCESSING = 2;

    /**
     * 任务成功
     */
    public static final int TASK_SUCCESS = 3;

    /**
     * 任务部分成功
     */
    public static final int TASK_PARTIAL_SUCCESS = 4;

    /**
     * 任务失败
     */
    public static final int TASK_FAILED = 5;

    /**
     * 任务用户待处理
     */
    public static final int TASK_USER_PENDING = 0;

    /**
     * 任务用户成功
     */
    public static final int TASK_USER_SUCCESS = 1;

    /**
     * 任务用户失败
     */
    public static final int TASK_USER_FAILED = 2;

    /**
     * 任务用户跳过
     */
    public static final int TASK_USER_SKIPPED = 3;

    private CouponGrantConstants() {
    }
}
