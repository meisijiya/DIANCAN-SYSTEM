package com.scaffold.modules.table.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 桌台区域实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("table_area")
public class TableArea extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 区域名称
     */
    private String name;

    /**
     * 区域排序
     */
    private Integer sort;

    /**
     * 状态（0禁用 1启用）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
