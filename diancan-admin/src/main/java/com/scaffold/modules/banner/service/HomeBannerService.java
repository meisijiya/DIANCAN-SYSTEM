package com.scaffold.modules.banner.service;

import com.scaffold.common.result.PageResult;
import com.scaffold.modules.banner.dto.HomeBannerCreateDTO;
import com.scaffold.modules.banner.dto.HomeBannerQueryDTO;
import com.scaffold.modules.banner.dto.HomeBannerUpdateDTO;
import com.scaffold.modules.banner.vo.HomeBannerVO;

import java.util.List;

/**
 * 小程序轮播图服务
 *
 * @author Henfon
 */
public interface HomeBannerService {

    /**
     * 分页查询轮播图
     *
     * @param dto 查询条件
     * @return 轮播图分页结果
     * @author Henfon
     * @date 2026-06-26
     * @description 管理端分页查询首页轮播图
     */
    PageResult<HomeBannerVO> pageList(HomeBannerQueryDTO dto);

    /**
     * 查询启用轮播图列表
     *
     * @return 轮播图列表
     * @author Henfon
     * @date 2026-06-26
     * @description 按投放位置查询启用状态的轮播图，首页、点餐页和我的页各自独立配置。
     */
    List<HomeBannerVO> listEnabled(String scene);

    /**
     * 创建轮播图
     *
     * @param dto 创建参数
     * @author Henfon
     * @date 2026-06-26
     * @description 新增首页轮播图配置
     */
    void create(HomeBannerCreateDTO dto);

    /**
     * 更新轮播图
     *
     * @param dto 更新参数
     * @author Henfon
     * @date 2026-06-26
     * @description 更新首页轮播图配置
     */
    void update(HomeBannerUpdateDTO dto);

    /**
     * 更新轮播图状态
     *
     * @param id 轮播图ID
     * @param status 状态
     * @author Henfon
     * @date 2026-06-26
     * @description 启用或停用轮播图
     */
    void updateStatus(Long id, Integer status);
}
