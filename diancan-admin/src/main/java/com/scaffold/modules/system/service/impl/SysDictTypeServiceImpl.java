package com.scaffold.modules.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.system.dto.DictTypeCreateDTO;
import com.scaffold.modules.system.dto.DictTypeQueryDTO;
import com.scaffold.modules.system.dto.DictTypeUpdateDTO;
import com.scaffold.modules.system.entity.SysDictType;
import com.scaffold.modules.system.mapper.SysDictDataMapper;
import com.scaffold.modules.system.mapper.SysDictTypeMapper;
import com.scaffold.modules.system.service.SysDictTypeService;
import com.scaffold.modules.system.vo.DictTypeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字典类型服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType> implements SysDictTypeService {

    private final SysDictDataMapper dictDataMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDictType(DictTypeCreateDTO dto) {
        // 检查编码是否存在
        if (existsByCode(dto.getCode())) {
            throw new BusinessException(ResultCode.DICT_TYPE_EXISTS);
        }

        SysDictType dictType = new SysDictType();
        BeanUtil.copyProperties(dto, dictType);
        save(dictType);
        log.info("字典类型创建成功: {}", dto.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDictType(DictTypeUpdateDTO dto) {
        SysDictType existType = getById(dto.getId());
        if (existType == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 检查编码是否被其他类型使用
        if (!existType.getCode().equals(dto.getCode()) && existsByCode(dto.getCode())) {
            throw new BusinessException(ResultCode.DICT_TYPE_EXISTS);
        }

        SysDictType dictType = new SysDictType();
        BeanUtil.copyProperties(dto, dictType);
        updateById(dictType);
        log.info("字典类型更新成功: {}", dto.getId());
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictType(Long dictTypeId) {
        SysDictType dictType = getById(dictTypeId);
        if (dictType == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 删除字典类型
        removeById(dictTypeId);
        // 级联删除字典数据
        dictDataMapper.deleteByTypeId(dictTypeId);
        log.info("字典类型删除成功: {}", dictTypeId);
    }

    @Override
    public PageResult<DictTypeVO> pageList(DictTypeQueryDTO dto) {
        Page<SysDictType> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(dto.getName()), SysDictType::getName, dto.getName())
                .like(StrUtil.isNotBlank(dto.getCode()), SysDictType::getCode, dto.getCode())
                .eq(dto.getStatus() != null, SysDictType::getStatus, dto.getStatus())
                .orderByDesc(SysDictType::getCreateTime);

        Page<SysDictType> result = page(page, wrapper);
        List<DictTypeVO> voList = BeanUtil.copyToList(result.getRecords(), DictTypeVO.class);
        return PageResult.of(voList, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    public List<DictTypeVO> listAll() {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysDictType::getId);
        List<SysDictType> types = list(wrapper);
        return BeanUtil.copyToList(types, DictTypeVO.class);
    }

    /**
     * 检查编码是否存在
     */
    private boolean existsByCode(String code) {
        return count(new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getCode, code)) > 0;
    }
}
