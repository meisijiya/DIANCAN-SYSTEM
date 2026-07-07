package com.scaffold.modules.print.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 打印机-分类映射实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("printer_category_mapping")
public class PrinterCategoryMapping extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 打印机ID
     */
    private Long printerId;

    /**
     * 菜品分类ID
     */
    private Long categoryId;
}
