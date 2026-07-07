package com.scaffold.modules.table.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.table.dto.TableAreaCreateDTO;
import com.scaffold.modules.table.dto.TableAreaUpdateDTO;
import com.scaffold.modules.table.entity.DiningTable;
import com.scaffold.modules.table.entity.TableArea;
import com.scaffold.modules.table.mapper.DiningTableMapper;
import com.scaffold.modules.table.mapper.TableAreaMapper;
import com.scaffold.modules.table.service.TableAreaService;
import com.scaffold.modules.table.vo.TableAreaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 桌台区域服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableAreaServiceImpl extends ServiceImpl<TableAreaMapper, TableArea> implements TableAreaService {

    private final DiningTableMapper diningTableMapper;

    @Override
    public List<TableAreaVO> listAll() {
        LambdaQueryWrapper<TableArea> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(TableArea::getSort)
                .orderByAsc(TableArea::getCreateTime);
        return BeanUtil.copyToList(list(wrapper), TableAreaVO.class);
    }

    @Override
    public List<TableAreaVO> listEnabled() {
        LambdaQueryWrapper<TableArea> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TableArea::getStatus, 1)
                .orderByAsc(TableArea::getSort)
                .orderByAsc(TableArea::getCreateTime);
        return BeanUtil.copyToList(list(wrapper), TableAreaVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createArea(TableAreaCreateDTO dto) {
        String areaName = normalizeAreaName(dto.getName());
        if (existsByName(areaName, null)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "区域名称已存在");
        }

        TableArea entity = new TableArea();
        BeanUtil.copyProperties(dto, entity);
        entity.setName(areaName);
        entity.setSort(dto.getSort() == null ? 0 : dto.getSort());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        save(entity);
        log.info("桌台区域创建成功: id={}, name={}", entity.getId(), entity.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArea(TableAreaUpdateDTO dto) {
        TableArea existArea = getById(dto.getId());
        if (existArea == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "区域不存在");
        }

        String nextAreaName = StrUtil.isBlank(dto.getName()) ? existArea.getName() : normalizeAreaName(dto.getName());
        if (existsByName(nextAreaName, dto.getId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "区域名称已存在");
        }

        TableArea updateEntity = new TableArea();
        BeanUtil.copyProperties(dto, updateEntity);
        updateEntity.setName(nextAreaName);
        updateById(updateEntity);

        if (!StrUtil.equals(existArea.getName(), nextAreaName)) {
            // 同步桌台表中的区域名称冗余字段，保证旧列表和二维码目录立即生效。
            LambdaUpdateWrapper<DiningTable> tableWrapper = new LambdaUpdateWrapper<>();
            tableWrapper.eq(DiningTable::getAreaId, dto.getId())
                    .set(DiningTable::getAreaName, nextAreaName);
            diningTableMapper.update(null, tableWrapper);
        }

        log.info("桌台区域更新成功: id={}, name={}", dto.getId(), nextAreaName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArea(Long id) {
        TableArea area = getById(id);
        if (area == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "区域不存在");
        }

        LambdaQueryWrapper<DiningTable> tableWrapper = new LambdaQueryWrapper<>();
        tableWrapper.eq(DiningTable::getAreaId, id);
        Long tableCount = diningTableMapper.selectCount(tableWrapper);
        if (tableCount != null && tableCount > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该区域下仍有关联桌台，不能删除");
        }

        removeById(id);
        log.info("桌台区域删除成功: id={}", id);
    }

    /**
     * 检查区域名称是否已存在
     *
     * @author Henfon
     * @date 2026-07-02
     * @description 通过逻辑删除过滤后的主数据表校验名称唯一性。
     * @param name 区域名称
     * @param excludeId 排除的区域ID
     * @return 是否存在
     */
    private boolean existsByName(String name, Long excludeId) {
        LambdaQueryWrapper<TableArea> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TableArea::getName, name)
                .ne(excludeId != null, TableArea::getId, excludeId);
        return count(wrapper) > 0;
    }

    /**
     * 规范化区域名称
     *
     * @author Henfon
     * @date 2026-07-02
     * @description 统一去除首尾空格，避免出现同名脏数据。
     * @param name 原始名称
     * @return 规范化后的名称
     */
    private String normalizeAreaName(String name) {
        return StrUtil.trim(name);
    }
}
