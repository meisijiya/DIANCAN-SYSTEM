package com.scaffold.modules.dish.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.dish.dto.DishSpecGroupCreateDTO;
import com.scaffold.modules.dish.dto.DishSpecGroupUpdateDTO;
import com.scaffold.modules.dish.dto.DishSpecOptionDTO;
import com.scaffold.modules.dish.entity.DishCategorySpec;
import com.scaffold.modules.dish.entity.DishSpecGroup;
import com.scaffold.modules.dish.entity.DishSpecMapping;
import com.scaffold.modules.dish.entity.DishSpecOption;
import com.scaffold.modules.dish.mapper.DishCategorySpecMapper;
import com.scaffold.modules.dish.mapper.DishSpecGroupMapper;
import com.scaffold.modules.dish.mapper.DishSpecMappingMapper;
import com.scaffold.modules.dish.mapper.DishSpecOptionMapper;
import com.scaffold.modules.dish.service.DishSpecGroupService;
import com.scaffold.modules.dish.vo.DishSpecGroupVO;
import com.scaffold.modules.dish.vo.DishSpecOptionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜品规格组服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DishSpecGroupServiceImpl implements DishSpecGroupService {

    private final DishSpecGroupMapper dishSpecGroupMapper;
    private final DishSpecOptionMapper dishSpecOptionMapper;
    private final DishCategorySpecMapper dishCategorySpecMapper;
    private final DishSpecMappingMapper dishSpecMappingMapper;

    @Override
    public List<DishSpecGroupVO> listAll() {
        LambdaQueryWrapper<DishSpecGroup> groupWrapper = new LambdaQueryWrapper<>();
        groupWrapper.orderByAsc(DishSpecGroup::getSort).orderByAsc(DishSpecGroup::getId);
        List<DishSpecGroup> groups = dishSpecGroupMapper.selectList(groupWrapper);
        if (groups.isEmpty()) {
            return List.of();
        }

        List<Long> groupIds = groups.stream().map(DishSpecGroup::getId).toList();
        LambdaQueryWrapper<DishSpecOption> optionWrapper = new LambdaQueryWrapper<>();
        optionWrapper.in(DishSpecOption::getGroupId, groupIds)
                .orderByAsc(DishSpecOption::getSort)
                .orderByAsc(DishSpecOption::getId);
        List<DishSpecOption> options = dishSpecOptionMapper.selectList(optionWrapper);

        Map<Long, List<DishSpecOptionVO>> optionMap = new LinkedHashMap<>();
        for (DishSpecOption option : options) {
            optionMap.computeIfAbsent(option.getGroupId(), key -> new ArrayList<>())
                    .add(BeanUtil.copyProperties(option, DishSpecOptionVO.class));
        }

        List<DishSpecGroupVO> result = new ArrayList<>(groups.size());
        for (DishSpecGroup group : groups) {
            DishSpecGroupVO vo = BeanUtil.copyProperties(group, DishSpecGroupVO.class);
            vo.setOptions(optionMap.getOrDefault(group.getId(), List.of()));
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createGroup(DishSpecGroupCreateDTO dto) {
        validateGroupPayload(dto.getName(), dto.getOptions());

        DishSpecGroup group = new DishSpecGroup();
        group.setName(dto.getName());
        group.setSort(dto.getSort() == null ? 0 : dto.getSort());
        group.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        dishSpecGroupMapper.insert(group);
        saveGroupOptions(group.getId(), dto.getOptions());
        log.info("菜品规格组创建成功: groupId={}, name={}", group.getId(), group.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGroup(DishSpecGroupUpdateDTO dto) {
        DishSpecGroup existing = dishSpecGroupMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "规格组不存在");
        }

        validateGroupPayload(dto.getName(), dto.getOptions());
        DishSpecGroup group = new DishSpecGroup();
        group.setId(dto.getId());
        group.setName(dto.getName());
        group.setSort(dto.getSort());
        group.setStatus(dto.getStatus());
        dishSpecGroupMapper.updateById(group);

        LambdaQueryWrapper<DishSpecOption> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(DishSpecOption::getGroupId, dto.getId());
        dishSpecOptionMapper.delete(deleteWrapper);
        saveGroupOptions(dto.getId(), dto.getOptions());
        log.info("菜品规格组更新成功: groupId={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long id) {
        DishSpecGroup existing = dishSpecGroupMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "规格组不存在");
        }

        LambdaQueryWrapper<DishCategorySpec> categoryWrapper = new LambdaQueryWrapper<>();
        categoryWrapper.eq(DishCategorySpec::getSpecGroupId, id);
        if (dishCategorySpecMapper.selectCount(categoryWrapper) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该规格组已关联分类，不能删除");
        }

        LambdaQueryWrapper<DishSpecMapping> mappingWrapper = new LambdaQueryWrapper<>();
        mappingWrapper.eq(DishSpecMapping::getSpecGroupId, id);
        if (dishSpecMappingMapper.selectCount(mappingWrapper) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该规格组已关联菜品，不能删除");
        }

        LambdaQueryWrapper<DishSpecOption> optionWrapper = new LambdaQueryWrapper<>();
        optionWrapper.eq(DishSpecOption::getGroupId, id);
        dishSpecOptionMapper.delete(optionWrapper);
        dishSpecGroupMapper.deleteById(id);
        log.info("菜品规格组删除成功: groupId={}", id);
    }

    /**
     * 校验规格组参数
     *
     * @param groupName 规格组名称
     * @param options 规格选项
     * @author Henfon
     * @date 2026-07-01
     * @description 统一校验规格组名称和选项集合，避免保存空名称或无选项规格组
     */
    private void validateGroupPayload(String groupName, List<DishSpecOptionDTO> options) {
        if (StrUtil.isBlank(groupName)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "规格组名称不能为空");
        }
        if (options == null || options.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请至少配置一个规格选项");
        }
        boolean hasBlankOption = options.stream().anyMatch(item -> StrUtil.isBlank(item.getName()));
        if (hasBlankOption) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "规格选项名称不能为空");
        }
    }

    /**
     * 保存规格组选项
     *
     * @param groupId 规格组ID
     * @param options 规格选项
     * @author Henfon
     * @date 2026-07-01
     * @description 按当前提交内容重建规格组选项，保持顺序与前端配置一致
     */
    private void saveGroupOptions(Long groupId, List<DishSpecOptionDTO> options) {
        LocalDateTime now = LocalDateTime.now();
        for (DishSpecOptionDTO optionDTO : options) {
            DishSpecOption option = new DishSpecOption();
            option.setId(IdWorker.getId());
            option.setGroupId(groupId);
            option.setName(optionDTO.getName());
            option.setSort(optionDTO.getSort() == null ? 0 : optionDTO.getSort());
            option.setCreateTime(now);
            option.setUpdateTime(now);
            option.setDeleted(0);
            dishSpecOptionMapper.insert(option);
        }
    }
}
