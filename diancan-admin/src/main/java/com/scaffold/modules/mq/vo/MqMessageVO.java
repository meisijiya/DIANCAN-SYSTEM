package com.scaffold.modules.mq.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MQ 消息 VO
 *
 * @author Henfon
 */
@Data
public class MqMessageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String messageKey;
    private String topic;
    private String tag;
    private String bizType;
    private String bizKey;
    private Integer deliverStatus;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private String lastError;
    private LocalDateTime sentTime;
    private LocalDateTime createTime;
    private Integer bizStatus;
    private String bizStatusText;
    private String bizStatusDetail;
}
