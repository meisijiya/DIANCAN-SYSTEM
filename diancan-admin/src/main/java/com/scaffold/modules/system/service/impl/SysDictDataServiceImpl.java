package com.scaffold.modules.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.common.constant.CacheConstants;
import com.scaffold.common.enums.StatusEnum;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.ResultCode;
import com.scaffold.framework.redis.RedisUtils;
import com.scaffold.modules.system.dto.DictDataCreateDTO;
import com.scaffold.modules.system.dto.DictDataUpdateDTO;
import com.scaffold.modules.system.entity.SysDictData;
import com.scaffold.modules.system.entity.SysDictType;
import com.scaffold.modules.system.mapper.SysDictDataMapper;
import com.scaffold.modules.system.mapper.SysDictTypeMapper;
import com.scaffold.modules.system.service.SysDictDataService;
import com.scaffold.modules.system.vo.DictDataVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字典数据服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements SysDictDataService {

    private final SysDictTypeMapper dictTypeMapper;
    private final RedisUtils redisUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDictData(DictDataCreateDTO dto) {
        SysDictData dictData = new SysDictData();
        BeanUtil.copyProperties(dto, dictData);
        save(dictData);
        
        // 刷新缓存
        refreshCacheByTypeId(dto.getTypeId());
        log.info("字典数据创建成功: {}", dto.getLabel());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDictData(DictDataUpdateDTO dto) {
        SysDictData existData = getById(dto.getId());
        if (existData == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        SysDictData dictData = new SysDictData();
        BeanUtil.copyProperties(dto, dictData);
        updateById(dictData);
        
        // 刷新缓存
        refreshCacheByTypeId(existData.getTypeId());
        log.info("字典数据更新成功: {}", dto.getId());
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictData(Long dictDataId) {
        SysDictData dictData = getById(dictDataId);
        if (dictData == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        removeById(dictDataId);
        
        // 刷新缓存
        refreshCacheByTypeId(dictData.getTypeId());
        log.info("字典数据删除成功: {}", dictDataId);
    }

    @Override
    public List<DictDataVO> getByTypeId(Long typeId) {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictData::getTypeId, typeId)
                .eq(SysDictData::getStatus, StatusEnum.ENABLED.getValue())
                .orderByAsc(SysDictData::getOrderNum);
        List<SysDictData> dataList = list(wrapper);
        return BeanUtil.copyToList(dataList, DictDataVO.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DictDataVO> getByTypeCode(String typeCode) {
        // 先从缓存获取
        String cacheKey = CacheConstants.DICT_KEY + typeCode;
        Object cached = redisUtils.get(cacheKey);
        if (cached != null) {
            return (List<DictDataVO>) cached;
        }

        // 查询字典类型
        LambdaQueryWrapper<SysDictType> typeWrapper = new LambdaQueryWrapper<>();
        typeWrapper.eq(SysDictType::getCode, typeCode);
        SysDictType dictType = dictTypeMapper.selectOne(typeWrapper);
        if (dictType == null) {
            return List.of();
        }

        // 查询字典数据
        List<DictDataVO> dataList = getByTypeId(dictType.getId());
        
        // 缓存结果
        redisUtils.set(cacheKey, dataList, CacheConstants.CACHE_EXPIRE);
        return dataList;
    }

    @Override
    public void refreshCache() {
        // 查询所有字典类型
        List<SysDictType> types = dictTypeMapper.selectList(null);
        for (SysDictType type : types) {
            refreshCacheByTypeId(type.getId());
        }
        log.info("字典缓存刷新完成");
    }

    /**
     * 根据类型ID刷新缓存
     */
    private void refreshCacheByTypeId(Long typeId) {
        LambdaQueryWrapper<SysDictType> typeWrapper = new LambdaQueryWrapper<>();
        typeWrapper.eq(SysDictType::getId, typeId);
        SysDictType dictType = dictTypeMapper.selectOne(typeWrapper);
        if (dictType != null) {
            String cacheKey = CacheConstants.DICT_KEY + dictType.getCode();
            List<DictDataVO> dataList = getByTypeId(typeId);
            redisUtils.set(cacheKey, dataList, CacheConstants.CACHE_EXPIRE);
        }
    }
}
