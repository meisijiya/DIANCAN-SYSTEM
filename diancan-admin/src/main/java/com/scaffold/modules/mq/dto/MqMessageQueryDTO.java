package com.scaffold.modules.mq.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * MQ 消息查询 DTO
 *
 * @author Henfon
 */
@Data
public class MqMessageQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 消息键
     */
    private String messageKey;

    /**
     * 投递状态
     */
    private Integer deliverStatus;

    /**
     * 业务结果状态
     */
    private Integer bizStatus;

    /**
     * 页码
     */
    private Long pageNum = 1L;

    /**
     * 每页数量
     */
    private Long pageSize = 10L;
}
