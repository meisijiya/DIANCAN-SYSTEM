package com.scaffold.modules.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 发券任务实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupon_grant_task")
public class CouponGrantTask extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 发放方式（1指定用户 2全部用户）
     */
    private Integer grantMode;

    /**
     * 任务状态（0待处理 1已完成 2部分完成）
     */
    private Integer taskStatus;

    /**
     * 目标人数
     */
    private Integer targetCount;

    /**
     * 成功人数
     */
    private Integer successCount;

    /**
     * 失败人数
     */
    private Integer failCount;

    /**
     * 备注
     */
    private String remark;

    /**
     * 开始处理时间
     */
    private LocalDateTime startedTime;

    /**
     * 最后错误信息
     */
    private String lastError;

    /**
     * 总批次数
     */
    private Integer totalBatchCount;

    /**
     * 已完成批次数
     */
    private Integer finishedBatchCount;

    /**
     * 完成时间
     */
    private LocalDateTime finishedTime;
}
