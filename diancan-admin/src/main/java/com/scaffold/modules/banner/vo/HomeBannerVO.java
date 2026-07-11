package com.scaffold.modules.banner.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 首页轮播图 VO
 *
 * @author Henfon
 */
@Data
public class HomeBannerVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private Integer actionType;
    private String targetPath;
    private String scene;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
}
