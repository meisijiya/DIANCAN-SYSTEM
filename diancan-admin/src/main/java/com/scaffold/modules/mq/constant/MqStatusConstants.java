package com.scaffold.modules.mq.constant;

/**
 * MQ 状态常量
 *
 * @author Henfon
 */
public final class MqStatusConstants {

    /**
     * 待投递
     */
    public static final int DELIVER_PENDING = 0;

    /**
     * 投递中
     */
    public static final int DELIVER_SENDING = 1;

    /**
     * 已投递
     */
    public static final int DELIVER_SENT = 2;

    /**
     * 投递失败
     */
    public static final int DELIVER_FAILED = 3;

    /**
     * 死信
     */
    public static final int DELIVER_DEAD = 4;

    /**
     * 消费处理中
     */
    public static final int CONSUME_PROCESSING = 0;

    /**
     * 消费成功
     */
    public static final int CONSUME_SUCCESS = 1;

    /**
     * 消费失败
     */
    public static final int CONSUME_FAILED = 2;

    private MqStatusConstants() {
    }
}
