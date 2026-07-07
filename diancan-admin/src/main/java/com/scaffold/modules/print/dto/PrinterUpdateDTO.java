package com.scaffold.modules.print.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 打印机更新 DTO
 *
 * @author Henfon
 */
@Data
public class PrinterUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 打印机ID
     */
    @NotNull(message = "打印机ID不能为空")
    private Long id;

    /**
     * 打印机名称
     */
    @Size(max = 50, message = "打印机名称长度不能超过50")
    private String name;

    /**
     * 打印机序列号
     */
    @Size(max = 100, message = "打印机序列号长度不能超过100")
    private String sn;

    /**
     * 类型（0前台 1后厨）
     */
    private Integer type;

    /**
     * 状态（0离线 1在线）
     */
    private Integer status;

    /**
     * 位置描述
     */
    @Size(max = 100, message = "位置描述长度不能超过100")
    private String location;
}
