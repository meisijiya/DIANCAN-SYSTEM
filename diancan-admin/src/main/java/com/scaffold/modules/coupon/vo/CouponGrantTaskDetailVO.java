package com.scaffold.modules.coupon.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 发券任务明细 VO
 *
 * @author Henfon
 */
@Data
public class CouponGrantTaskDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long taskId;
    private Long userId;
    private String username;
    private String phone;
    private Integer grantStatus;
    private String failReason;
    private LocalDateTime finishedTime;
    private LocalDateTime createTime;
}
