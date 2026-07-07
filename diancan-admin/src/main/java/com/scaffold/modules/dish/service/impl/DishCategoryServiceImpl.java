package com.scaffold.modules.dish.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.dish.dto.DishCategoryCreateDTO;
import com.scaffold.modules.dish.dto.DishCategorySortDTO;
import com.scaffold.modules.dish.dto.DishCategoryUpdateDTO;
import com.scaffold.modules.dish.entity.DishCategory;
import com.scaffold.modules.dish.entity.DishCategorySpec;
import com.scaffold.modules.dish.entity.DishSpecGroup;
import com.scaffold.modules.dish.mapper.DishCategorySpecMapper;
import com.scaffold.modules.dish.mapper.DishCategoryMapper;
import com.scaffold.modules.dish.mapper.DishSpecGroupMapper;
import com.scaffold.modules.dish.service.DishCategoryService;
import com.scaffold.modules.dish.vo.DishCategoryVO;
import com.scaffold.modules.system.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜品分类服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DishCategoryServiceImpl extends ServiceImpl<DishCategoryMapper, DishCategory> implements DishCategoryService {

    private final DishCategorySpecMapper dishCategorySpecMapper;
    private final DishSpecGroupMapper dishSpecGroupMapper;
    private final MinioStorageService minioStorageService;

    @Override
    public List<DishCategoryVO> listEnabled() {
        LambdaQueryWrapper<DishCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishCategory::getStatus, 1)
                .orderByAsc(DishCategory::getSort)
                .orderByAsc(DishCategory::getId);
        List<DishCategory> categories = list(wrapper);
        return fillCategorySpecInfo(categories);
    }

    @Override
    public List<DishCategoryVO> listAll() {
        LambdaQueryWrapper<DishCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(DishCategory::getSort)
                .orderByAsc(DishCategory::getId);
        List<DishCategory> categories = list(wrapper);
        return fillCategorySpecInfo(categories);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCategory(DishCategoryCreateDTO dto) {
        DishCategory category = new DishCategory();
        BeanUtil.copyProperties(dto, category);
        category.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        category.setImage(minioStorageService.normalizeObjectKey(dto.getImage()));
        save(category);
        saveCategorySpecMappings(category.getId(), dto.getSpecGroupIds());
        log.info("菜品分类创建成功: {}", dto.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(DishCategoryUpdateDTO dto) {
        DishCategory existCategory = getById(dto.getId());
        if (existCategory == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        DishCategory category = new DishCategory();
        BeanUtil.copyProperties(dto, category);
        category.setImage(minioStorageService.normalizeObjectKey(dto.getImage()));
        updateById(category);
        saveCategorySpecMappings(dto.getId(), dto.getSpecGroupIds());
        log.info("菜品分类更新成功: {}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        DishCategory category = getById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        removeById(id);
        log.info("菜品分类删除成功: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSort(DishCategorySortDTO dto) {
        List<DishCategory> updateList = dto.getItems().stream().map(item -> {
            DishCategory category = new DishCategory();
            category.setId(item.getId());
            category.setSort(item.getSort());
            return category;
        }).toList();

        updateBatchById(updateList);
        log.info("菜品分类排序更新成功，共 {} 条", updateList.size());
    }

    /**
     * 填充分类规格信息
     *
     * @param categories 分类实体列表
     * @return 分类 VO 列表
     * @author Henfon
     * @date 2026-07-01
     * @description 批量装载分类关联的默认规格组 ID 与名称，供前端直接展示和编辑
     */
    private List<DishCategoryVO> fillCategorySpecInfo(List<DishCategory> categories) {
        List<DishCategoryVO> result = BeanUtil.copyToList(categories, DishCategoryVO.class);
        if (categories.isEmpty()) {
            return result;
        }

        List<Long> categoryIds = categories.stream().map(DishCategory::getId).toList();
        LambdaQueryWrapper<DishCategorySpec> mappingWrapper = new LambdaQueryWrapper<>();
        mappingWrapper.in(DishCategorySpec::getCategoryId, categoryIds)
                .orderByAsc(DishCategorySpec::getId);
        List<DishCategorySpec> mappings = dishCategorySpecMapper.selectList(mappingWrapper);
        if (mappings.isEmpty()) {
            return result;
        }

        List<Long> specGroupIds = mappings.stream().map(DishCategorySpec::getSpecGroupId).distinct().toList();
        LambdaQueryWrapper<DishSpecGroup> groupWrapper = new LambdaQueryWrapper<>();
        groupWrapper.in(DishSpecGroup::getId, specGroupIds);
        List<DishSpecGroup> groups = dishSpecGroupMapper.selectList(groupWrapper);
        Map<Long, String> groupNameMap = new LinkedHashMap<>();
        for (DishSpecGroup group : groups) {
            groupNameMap.put(group.getId(), group.getName());
        }

        Map<Long, List<Long>> categorySpecGroupIdMap = new LinkedHashMap<>();
        Map<Long, List<String>> categorySpecGroupNameMap = new LinkedHashMap<>();
        for (DishCategorySpec mapping : mappings) {
            categorySpecGroupIdMap.computeIfAbsent(mapping.getCategoryId(), key -> new ArrayList<>()).add(mapping.getSpecGroupId());
            String groupName = groupNameMap.get(mapping.getSpecGroupId());
            if (groupName != null) {
                categorySpecGroupNameMap.computeIfAbsent(mapping.getCategoryId(), key -> new ArrayList<>()).add(groupName);
            }
        }

        for (DishCategoryVO item : result) {
            item.setSpecGroupIds(categorySpecGroupIdMap.getOrDefault(item.getId(), List.of()));
            item.setSpecGroupNames(categorySpecGroupNameMap.getOrDefault(item.getId(), List.of()));
            // 分类图片支持存对象键，返回前统一补全为可访问地址
            item.setImage(minioStorageService.resolveAccessUrl(item.getImage()));
        }
        return result;
    }

    /**
     * 保存分类默认规格映射
     *
     * @param categoryId 分类ID
     * @param specGroupIds 规格组ID列表
     * @author Henfon
     * @date 2026-07-01
     * @description 先删除旧关联，再按前端选择重建分类默认规格组
     */
    private void saveCategorySpecMappings(Long categoryId, List<Long> specGroupIds) {
        // 这里必须物理删除旧映射。dish_category_spec 走逻辑删除时，唯一索引仍会占用 category_id + spec_group_id。
        dishCategorySpecMapper.deleteByCategoryIdPhysical(categoryId);

        if (specGroupIds == null || specGroupIds.isEmpty()) {
            return;
        }

        List<Long> distinctSpecGroupIds = specGroupIds.stream()
                .filter(item -> item != null && item > 0)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));
        if (distinctSpecGroupIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Long specGroupId : distinctSpecGroupIds) {
            DishCategorySpec mapping = new DishCategorySpec();
            mapping.setId(IdWorker.getId());
            mapping.setCategoryId(categoryId);
            mapping.setSpecGroupId(specGroupId);
            mapping.setCreateTime(now);
            mapping.setUpdateTime(now);
            mapping.setDeleted(0);
            dishCategorySpecMapper.insert(mapping);
        }
    }
}
