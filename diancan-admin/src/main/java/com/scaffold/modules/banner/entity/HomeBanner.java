package com.scaffold.modules.banner.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 首页轮播图实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("home_banner")
public class HomeBanner extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主标题
     */
    private String title;

    /**
     * 副标题
     */
    private String subtitle;

    /**
     * 图片地址或对象键
     */
    private String imageUrl;

    /**
     * 操作类型（0无动作 1页面跳转 2切换Tab）
     */
    private Integer actionType;

    /**
     * 跳转路径
     */
    private String targetPath;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态（0停用 1启用）
     */
    private Integer status;
}
