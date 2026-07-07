package com.scaffold.modules.member.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 积分兑换优惠券配置保存 DTO
 *
 * @author Henfon
 */
@Data
public class MemberCouponExchangeSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull(message = "优惠券模板不能为空")
    private Long templateId;

    @NotNull(message = "兑换积分不能为空")
    @Min(value = 1, message = "兑换积分必须大于0")
    private Integer pointsCost;

    @NotNull(message = "每人兑换次数不能为空")
    @Min(value = 0, message = "每人兑换次数不能小于0")
    private Integer perUserLimit;

    @NotNull(message = "排序不能为空")
    @Min(value = 0, message = "排序不能小于0")
    private Integer sort;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String remark;
}
