package com.scaffold.modules.banner.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.banner.dto.HomeBannerCreateDTO;
import com.scaffold.modules.banner.dto.HomeBannerQueryDTO;
import com.scaffold.modules.banner.dto.HomeBannerUpdateDTO;
import com.scaffold.modules.banner.entity.HomeBanner;
import com.scaffold.modules.banner.mapper.HomeBannerMapper;
import com.scaffold.modules.banner.service.HomeBannerService;
import com.scaffold.modules.banner.vo.HomeBannerVO;
import com.scaffold.modules.system.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 小程序轮播图服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeBannerServiceImpl implements HomeBannerService {

    private static final String SCENE_HOME = "HOME";
    private static final String SCENE_MENU_HERO = "MENU_HERO";
    private static final String SCENE_MENU_BANNER = "MENU_BANNER";
    private static final String SCENE_PROFILE_HERO = "PROFILE_HERO";
    private static final Set<String> SUPPORTED_SCENES = Set.of(
            SCENE_HOME,
            SCENE_MENU_HERO,
            SCENE_MENU_BANNER,
            SCENE_PROFILE_HERO
    );

    private final HomeBannerMapper homeBannerMapper;
    private final MinioStorageService minioStorageService;

    @Override
    public PageResult<HomeBannerVO> pageList(HomeBannerQueryDTO dto) {
        Page<HomeBanner> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        String queryScene = StrUtil.isNotBlank(dto.getScene()) ? normalizeScene(dto.getScene(), false) : null;
        LambdaQueryWrapper<HomeBanner> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(dto.getTitle()), HomeBanner::getTitle, dto.getTitle())
                .eq(StrUtil.isNotBlank(queryScene), HomeBanner::getScene, queryScene)
                .eq(dto.getStatus() != null, HomeBanner::getStatus, dto.getStatus())
                .orderByAsc(HomeBanner::getSort)
                .orderByDesc(HomeBanner::getCreateTime);

        Page<HomeBanner> result = homeBannerMapper.selectPage(page, wrapper);
        List<HomeBannerVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    public List<HomeBannerVO> listEnabled(String scene) {
        String normalizedScene = normalizeScene(scene, true);
        LambdaQueryWrapper<HomeBanner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HomeBanner::getStatus, 1)
                .eq(HomeBanner::getScene, normalizedScene)
                .orderByAsc(HomeBanner::getSort)
                .orderByDesc(HomeBanner::getCreateTime);
        return homeBannerMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(HomeBannerCreateDTO dto) {
        validateBanner(dto);

        HomeBanner banner = new HomeBanner();
        BeanUtil.copyProperties(dto, banner);
        // 我的页 HERO 按纯图片投放时允许空标题，统一落库为空字符串以兼容表结构约束。
        banner.setTitle(StrUtil.blankToDefault(dto.getTitle(), ""));
        banner.setImageUrl(minioStorageService.normalizeObjectKey(dto.getImageUrl()));
        banner.setScene(normalizeScene(dto.getScene(), false));
        homeBannerMapper.insert(banner);
        log.info("轮播图创建成功: id={}, title={}, scene={}", banner.getId(), banner.getTitle(), banner.getScene());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(HomeBannerUpdateDTO dto) {
        HomeBanner exist = getBannerOrThrow(dto.getId());
        validateBanner(dto);

        // 我的页 HERO 按纯图片投放时允许空标题，统一落库为空字符串以兼容表结构约束。
        exist.setTitle(StrUtil.blankToDefault(dto.getTitle(), ""));
        exist.setSubtitle(dto.getSubtitle());
        exist.setImageUrl(minioStorageService.normalizeObjectKey(dto.getImageUrl()));
        exist.setActionType(dto.getActionType());
        exist.setTargetPath(dto.getTargetPath());
        exist.setScene(normalizeScene(dto.getScene(), false));
        exist.setSort(dto.getSort());
        exist.setStatus(dto.getStatus());
        homeBannerMapper.updateById(exist);
        log.info("轮播图更新成功: id={}, scene={}", dto.getId(), exist.getScene());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        getBannerOrThrow(id);
        LambdaUpdateWrapper<HomeBanner> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HomeBanner::getId, id)
                .set(HomeBanner::getStatus, status);
        homeBannerMapper.update(null, wrapper);
        log.info("首页轮播图状态更新成功: id={}, status={}", id, status);
    }

    /**
     * 查询轮播图并校验存在性
     *
     * @param id 轮播图ID
     * @return 轮播图实体
     * @author Henfon
     * @date 2026-06-26
     * @description 更新前统一校验轮播图是否存在
     */
    private HomeBanner getBannerOrThrow(Long id) {
        HomeBanner banner = homeBannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "轮播图不存在");
        }
        return banner;
    }

    /**
     * 校验轮播图参数
     *
     * @param dto 轮播图参数
     * @author Henfon
     * @date 2026-06-26
     * @description 校验轮播图场景、标题和跳转配置，避免保存无效轮播图
     */
    private void validateBanner(HomeBannerCreateDTO dto) {
        String normalizedScene = normalizeScene(dto.getScene(), false);
        if (dto.getSort() == null || dto.getSort() < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "排序不能小于0");
        }
        if (!SCENE_PROFILE_HERO.equals(normalizedScene) && StrUtil.isBlank(dto.getTitle())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "主标题不能为空");
        }
        if (dto.getActionType() != null && dto.getActionType() != 0 && StrUtil.isBlank(dto.getTargetPath())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "配置跳转动作时必须填写跳转路径");
        }
    }

    /**
     * 规范化投放位置
     *
     * @author Henfon
     * @date 2026-07-11
     * @description 统一校验并标准化轮播图投放位置；小程序查询未传时默认回落到首页位置。
     * @param scene 原始投放位置
     * @param useDefaultWhenBlank 空值时是否回落默认首页位置
     * @return 规范化后的投放位置
     */
    private String normalizeScene(String scene, boolean useDefaultWhenBlank) {
        String normalizedScene = StrUtil.trimToEmpty(scene).toUpperCase();
        if (StrUtil.isBlank(normalizedScene)) {
            if (useDefaultWhenBlank) {
                return SCENE_HOME;
            }
            throw new BusinessException(ResultCode.PARAM_ERROR, "投放位置不能为空");
        }

        if (!SUPPORTED_SCENES.contains(normalizedScene)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "投放位置不正确");
        }
        return normalizedScene;
    }

    /**
     * 转换轮播图 VO
     *
     * @param banner 轮播图实体
     * @return 轮播图 VO
     * @author Henfon
     * @date 2026-06-26
     * @description 统一补全图片访问地址，供管理端和小程序复用
     */
    private HomeBannerVO toVO(HomeBanner banner) {
        HomeBannerVO vo = BeanUtil.copyProperties(banner, HomeBannerVO.class);
        vo.setImageUrl(minioStorageService.resolveAccessUrl(banner.getImageUrl()));
        return vo;
    }
}
