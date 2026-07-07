package com.scaffold.modules.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分单结账子项 DTO
 *
 * @author Henfon
 */
@Data
@Schema(description = "分单结账子项")
public class SplitBillItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订单项ID列表")
    private List<Long> orderItemIds;

    @Schema(description = "单个订单项ID（兼容旧版请求）")
    private Long orderItemId;

    @Schema(description = "支付方式（0微信 1支付宝 2现金）")
    private Integer paymentMethod;
}
