package com.scaffold.modules.print.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 打印机创建 DTO
 *
 * @author Henfon
 */
@Data
public class PrinterCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 打印机名称
     */
    @NotBlank(message = "打印机名称不能为空")
    @Size(max = 50, message = "打印机名称长度不能超过50")
    private String name;

    /**
     * 打印机序列号
     */
    @NotBlank(message = "打印机序列号不能为空")
    @Size(max = 100, message = "打印机序列号长度不能超过100")
    private String sn;

    /**
     * 类型（0前台 1后厨）
     */
    @NotNull(message = "打印机类型不能为空")
    private Integer type;

    /**
     * 位置描述
     */
    @Size(max = 100, message = "位置描述长度不能超过100")
    private String location;
}
