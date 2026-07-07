package com.scaffold.modules.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 发券任务用户快照实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupon_grant_task_user")
public class CouponGrantTaskUser extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名快照
     */
    private String username;

    /**
     * 手机号快照
     */
    private String phone;

    /**
     * 发放状态（0待处理 1成功 2失败 3跳过）
     */
    private Integer grantStatus;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 处理完成时间
     */
    private LocalDateTime finishedTime;
}
