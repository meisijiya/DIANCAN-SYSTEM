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

/**
 * 首页轮播图服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeBannerServiceImpl implements HomeBannerService {

    private final HomeBannerMapper homeBannerMapper;
    private final MinioStorageService minioStorageService;

    @Override
    public PageResult<HomeBannerVO> pageList(HomeBannerQueryDTO dto) {
        Page<HomeBanner> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<HomeBanner> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(dto.getTitle()), HomeBanner::getTitle, dto.getTitle())
                .eq(dto.getStatus() != null, HomeBanner::getStatus, dto.getStatus())
                .orderByAsc(HomeBanner::getSort)
                .orderByDesc(HomeBanner::getCreateTime);

        Page<HomeBanner> result = homeBannerMapper.selectPage(page, wrapper);
        List<HomeBannerVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    public List<HomeBannerVO> listEnabled() {
        LambdaQueryWrapper<HomeBanner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HomeBanner::getStatus, 1)
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
        banner.setImageUrl(minioStorageService.normalizeObjectKey(dto.getImageUrl()));
        homeBannerMapper.insert(banner);
        log.info("首页轮播图创建成功: id={}, title={}", banner.getId(), banner.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(HomeBannerUpdateDTO dto) {
        HomeBanner exist = getBannerOrThrow(dto.getId());
        validateBanner(dto);

        exist.setTitle(dto.getTitle());
        exist.setSubtitle(dto.getSubtitle());
        exist.setImageUrl(minioStorageService.normalizeObjectKey(dto.getImageUrl()));
        exist.setActionType(dto.getActionType());
        exist.setTargetPath(dto.getTargetPath());
        exist.setSort(dto.getSort());
        exist.setStatus(dto.getStatus());
        homeBannerMapper.updateById(exist);
        log.info("首页轮播图更新成功: id={}", dto.getId());
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
     * @description 校验跳转配置和排序字段，避免保存无效轮播图
     */
    private void validateBanner(HomeBannerCreateDTO dto) {
        if (dto.getSort() == null || dto.getSort() < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "排序不能小于0");
        }
        if (dto.getActionType() != null && dto.getActionType() != 0 && StrUtil.isBlank(dto.getTargetPath())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "配置跳转动作时必须填写跳转路径");
        }
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
