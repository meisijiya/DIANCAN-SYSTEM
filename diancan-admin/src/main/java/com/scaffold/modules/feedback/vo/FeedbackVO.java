package com.scaffold.modules.feedback.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 小程序反馈 VO
 *
 * @author Henfon
 */
@Data
public class FeedbackVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String content;
    private String contactPhone;
    private Integer status;
    private String replyContent;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
}
