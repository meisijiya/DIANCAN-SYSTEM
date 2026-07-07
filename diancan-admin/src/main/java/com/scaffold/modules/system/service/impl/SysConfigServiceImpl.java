package com.scaffold.modules.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.common.constant.CacheConstants;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.ResultCode;
import com.scaffold.framework.redis.RedisUtils;
import com.scaffold.modules.system.dto.ConfigCreateDTO;
import com.scaffold.modules.system.dto.ConfigQueryDTO;
import com.scaffold.modules.system.dto.ConfigUpdateDTO;
import com.scaffold.modules.system.entity.SysConfig;
import com.scaffold.modules.system.mapper.SysConfigMapper;
import com.scaffold.modules.system.service.SysConfigService;
import com.scaffold.modules.system.vo.ConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统配置服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    private final RedisUtils redisUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createConfig(ConfigCreateDTO dto) {
        // 检查配置键是否存在
        if (existsByKey(dto.getConfigKey())) {
            throw new BusinessException(ResultCode.CONFIG_KEY_EXISTS);
        }

        SysConfig config = new SysConfig();
        BeanUtil.copyProperties(dto, config);
        save(config);
        
        // 更新缓存
        updateCache(dto.getConfigKey(), dto.getConfigValue());
        log.info("配置创建成功: {}", dto.getConfigKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(ConfigUpdateDTO dto) {
        SysConfig existConfig = getById(dto.getId());
        if (existConfig == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 检查配置键是否被其他配置使用
        if (!existConfig.getConfigKey().equals(dto.getConfigKey()) && existsByKey(dto.getConfigKey())) {
            throw new BusinessException(ResultCode.CONFIG_KEY_EXISTS);
        }

        // 如果配置键变更，删除旧缓存
        if (!existConfig.getConfigKey().equals(dto.getConfigKey())) {
            deleteCache(existConfig.getConfigKey());
        }

        SysConfig config = new SysConfig();
        BeanUtil.copyProperties(dto, config);
        updateById(config);
        
        // 更新缓存
        updateCache(dto.getConfigKey(), dto.getConfigValue());
        log.info("配置更新成功: {}", dto.getId());
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long configId) {
        SysConfig config = getById(configId);
        if (config == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        removeById(configId);
        
        // 删除缓存
        deleteCache(config.getConfigKey());
        log.info("配置删除成功: {}", configId);
    }

    @Override
    public PageResult<ConfigVO> pageList(ConfigQueryDTO dto) {
        Page<SysConfig> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(dto.getName()), SysConfig::getName, dto.getName())
                .like(StrUtil.isNotBlank(dto.getConfigKey()), SysConfig::getConfigKey, dto.getConfigKey())
                .orderByDesc(SysConfig::getCreateTime);

        Page<SysConfig> result = page(page, wrapper);
        List<ConfigVO> voList = BeanUtil.copyToList(result.getRecords(), ConfigVO.class);
        return PageResult.of(voList, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    public String getConfigValue(String configKey) {
        // 先从缓存获取
        String cacheKey = CacheConstants.CONFIG_KEY + configKey;
        Object cached = redisUtils.get(cacheKey);
        if (cached != null) {
            return cached.toString();
        }

        // 从数据库查询
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey);
        SysConfig config = getOne(wrapper);
        if (config == null) {
            return null;
        }

        // 缓存结果
        redisUtils.set(cacheKey, config.getConfigValue(), CacheConstants.CACHE_EXPIRE);
        return config.getConfigValue();
    }

    /**
     * 按配置键保存配置值
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 配置不存在时自动创建，存在时覆盖更新，并同步刷新缓存。
     * @param configKey 配置键
     * @param configValue 配置值
     * @param name 配置名称
     * @param remark 配置备注
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfigValue(String configKey, String configValue, String name, String remark) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey);
        SysConfig config = getOne(wrapper);

        if (config == null) {
            config = new SysConfig();
            config.setName(name);
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setRemark(remark);
            save(config);
        } else {
            config.setName(name);
            config.setConfigValue(configValue);
            config.setRemark(remark);
            updateById(config);
        }

        // 写库后立即刷新缓存，确保开关实时生效。
        updateCache(configKey, configValue);
        log.info("配置按键保存成功: key={}, value={}", configKey, configValue);
    }

    @Override
    public void refreshCache() {
        // 查询所有配置
        List<SysConfig> configs = list();
        for (SysConfig config : configs) {
            updateCache(config.getConfigKey(), config.getConfigValue());
        }
        log.info("配置缓存刷新完成");
    }

    /**
     * 检查配置键是否存在
     */
    private boolean existsByKey(String configKey) {
        return count(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, configKey)) > 0;
    }

    /**
     * 更新缓存
     */
    private void updateCache(String configKey, String configValue) {
        String cacheKey = CacheConstants.CONFIG_KEY + configKey;
        redisUtils.set(cacheKey, configValue, CacheConstants.CACHE_EXPIRE);
    }

    /**
     * 删除缓存
     */
    private void deleteCache(String configKey) {
        String cacheKey = CacheConstants.CONFIG_KEY + configKey;
        redisUtils.delete(cacheKey);
    }
}
