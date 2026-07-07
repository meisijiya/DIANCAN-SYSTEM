package com.scaffold.modules.dish.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.ResultCode;
import com.scaffold.framework.redis.RedisUtils;
import com.scaffold.modules.dish.dto.DishCreateDTO;
import com.scaffold.modules.dish.dto.DishQueryDTO;
import com.scaffold.modules.dish.dto.DishSpecItemDTO;
import com.scaffold.modules.dish.dto.DishUpdateDTO;
import com.scaffold.modules.dish.entity.Dish;
import com.scaffold.modules.dish.entity.DishCategory;
import com.scaffold.modules.dish.entity.DishSpecGroup;
import com.scaffold.modules.dish.entity.DishSpecMapping;
import com.scaffold.modules.dish.entity.DishSpecOption;
import com.scaffold.modules.dish.mapper.DishMapper;
import com.scaffold.modules.dish.mapper.DishSpecGroupMapper;
import com.scaffold.modules.dish.mapper.DishSpecMappingMapper;
import com.scaffold.modules.dish.mapper.DishSpecOptionMapper;
import com.scaffold.modules.dish.service.DishCategoryService;
import com.scaffold.modules.dish.service.DishService;
import com.scaffold.modules.dish.vo.DishListVO;
import com.scaffold.modules.dish.vo.DishSpecItemVO;
import com.scaffold.modules.dish.vo.DishVO;
import com.scaffold.modules.system.service.MinioStorageService;
import com.scaffold.common.enums.WsEventType;
import com.scaffold.framework.websocket.WsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 菜品服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    private final DishCategoryService dishCategoryService;
    private final RedisUtils redisUtils;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WsService wsService;
    private final MinioStorageService minioStorageService;
    private final DishSpecMappingMapper dishSpecMappingMapper;
    private final DishSpecGroupMapper dishSpecGroupMapper;
    private final DishSpecOptionMapper dishSpecOptionMapper;

    private static final String DISH_LIST_CACHE_KEY = "dish:list";
    private static final String DISH_SOLD_OUT_KEY = "dish:sold-out";
    private static final long DISH_LIST_CACHE_TTL = 10;

    @Override
    @SuppressWarnings("unchecked")
    public Map<Long, List<DishListVO>> listOnSaleDishes() {
        // 优先从 Redis 缓存读取
        Object cached = redisUtils.get(DISH_LIST_CACHE_KEY);
        if (cached instanceof Map) {
            return (Map<Long, List<DishListVO>>) cached;
        }

        // 查询上架且未售罄的菜品
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getStatus, 1)
                .eq(Dish::getSoldOut, 0)
                .orderByAsc(Dish::getCategoryId)
                .orderByAsc(Dish::getId);
        List<Dish> dishes = list(wrapper);

        // 按分类ID分组
        Map<Long, List<DishListVO>> result = dishes.stream()
                .map(dish -> {
                    DishListVO vo = BeanUtil.copyProperties(dish, DishListVO.class);
                    vo.setThumbnail(minioStorageService.resolveAccessUrl(dish.getThumbnail()));
                    return vo;
                })
                .collect(Collectors.groupingBy(vo -> {
                    // 从原始数据中获取 categoryId
                    Dish original = dishes.stream()
                            .filter(d -> d.getId().equals(vo.getId()))
                            .findFirst().orElse(null);
                    return original != null ? original.getCategoryId() : 0L;
                }));

        // 写入缓存
        redisUtils.set(DISH_LIST_CACHE_KEY, result, DISH_LIST_CACHE_TTL, TimeUnit.MINUTES);
        return result;
    }

    @Override
    public List<DishVO> searchDishes(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return List.of();
        }

        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getStatus, 1)
                .and(w -> w.like(Dish::getName, keyword)
                        .or()
                        .like(Dish::getIngredients, keyword))
                .orderByAsc(Dish::getId);
        List<Dish> dishes = list(wrapper);
        return toDishVOList(dishes);
    }

    @Override
    public DishVO getDishDetail(Long id) {
        Dish dish = getById(id);
        if (dish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return toDishVO(dish);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDish(DishCreateDTO dto) {
        Dish dish = new Dish();
        BeanUtil.copyProperties(dto, dish);
        dish.setImage(minioStorageService.normalizeObjectKey(dto.getImage()));
        dish.setThumbnail(minioStorageService.normalizeObjectKey(dto.getThumbnail()));
        dish.setStatus(1);
        dish.setSoldOut(0);
        save(dish);
        saveDishSpecMappings(dish.getId(), dto.getSpecItems());

        // 清除菜品列表缓存
        redisUtils.delete(DISH_LIST_CACHE_KEY);
        log.info("菜品创建成功: {}", dto.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDish(DishUpdateDTO dto) {
        Dish existDish = getById(dto.getId());
        if (existDish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        Dish dish = new Dish();
        BeanUtil.copyProperties(dto, dish);
        dish.setImage(minioStorageService.normalizeObjectKey(dto.getImage()));
        dish.setThumbnail(minioStorageService.normalizeObjectKey(dto.getThumbnail()));
        updateById(dish);
        saveDishSpecMappings(dto.getId(), dto.getSpecItems());

        // 清除菜品列表缓存
        redisUtils.delete(DISH_LIST_CACHE_KEY);
        log.info("菜品更新成功: {}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDishStatus(Long id, Integer status) {
        Dish dish = getById(id);
        if (dish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        LambdaUpdateWrapper<Dish> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Dish::getId, id)
                .set(Dish::getStatus, status);
        update(wrapper);

        // 清除菜品列表缓存
        redisUtils.delete(DISH_LIST_CACHE_KEY);
        log.info("菜品状态更新成功: id={}, status={}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSoldOut(Long dishId, Integer soldOut) {
        Dish dish = getById(dishId);
        if (dish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        LambdaUpdateWrapper<Dish> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Dish::getId, dishId)
                .set(Dish::getSoldOut, soldOut);
        update(wrapper);

        // 更新 Redis 售罄集合
        if (soldOut == 1) {
            redisUtils.sAdd(DISH_SOLD_OUT_KEY, dishId);
        } else {
            redisTemplate.opsForSet().remove(DISH_SOLD_OUT_KEY, dishId);
        }

        // 清除菜品列表缓存
        redisUtils.delete(DISH_LIST_CACHE_KEY);

        // 通过 WebSocket 广播 SOLD_OUT 事件至所有终端（顾客端、服务端、后厨端）
        Map<String, Object> soldOutData = new HashMap<>();
        soldOutData.put("dishId", dishId);
        soldOutData.put("dishName", dish.getName());
        soldOutData.put("soldOut", soldOut);
        wsService.broadcast(WsEventType.SOLD_OUT, "/topic/sold-out", soldOutData);

        log.info("菜品估清状态更新: id={}, soldOut={}", dishId, soldOut);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStock(Long dishId, int quantity) {
        Dish dish = getById(dishId);
        if (dish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 不限库存
        if (dish.getStock() == -1) {
            return true;
        }

        // 使用 Redis DECRBY 原子操作扣减库存
        String stockKey = "dish:stock:" + dishId;
        // 初始化 Redis 库存（如果不存在）
        Boolean hasKey = redisUtils.hasKey(stockKey);
        if (Boolean.FALSE.equals(hasKey)) {
            redisUtils.set(stockKey, dish.getStock());
        }

        Long remaining = redisTemplate.opsForValue().decrement(stockKey, quantity);
        if (remaining == null || remaining < 0) {
            // 回滚 Redis 库存
            redisTemplate.opsForValue().increment(stockKey, quantity);
            throw new BusinessException(ResultCode.DISH_STOCK_NOT_ENOUGH);
        }

        // 同步更新数据库库存
        LambdaUpdateWrapper<Dish> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Dish::getId, dishId)
                .set(Dish::getStock, remaining.intValue());
        update(wrapper);

        // 库存为0时自动触发估清
        if (remaining == 0) {
            markSoldOut(dishId, 1);
            log.info("菜品库存为0，自动估清: id={}", dishId);
        }

        // 清除菜品列表缓存
        redisUtils.delete(DISH_LIST_CACHE_KEY);
        return true;
    }

    @Override
    public IPage<DishVO> listDishesForAdmin(int pageNum, int pageSize, DishQueryDTO queryDTO) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO != null) {
            if (queryDTO.getCategoryId() != null) {
                wrapper.eq(Dish::getCategoryId, queryDTO.getCategoryId());
            }
            if (StrUtil.isNotBlank(queryDTO.getName())) {
                wrapper.like(Dish::getName, queryDTO.getName());
            }
            if (queryDTO.getStatus() != null) {
                wrapper.eq(Dish::getStatus, queryDTO.getStatus());
            }
        }

        wrapper.orderByDesc(Dish::getCreateTime);

        IPage<Dish> page = page(new Page<>(pageNum, pageSize), wrapper);

        // 转换为 VO
        return page.convert(this::toDishVO);
    }

    /**
     * 将 Dish 实体转换为 DishVO（含分类名称）
     */
    private DishVO toDishVO(Dish dish) {
        DishVO vo = BeanUtil.copyProperties(dish, DishVO.class);
        // 填充分类名称
        if (dish.getCategoryId() != null) {
            DishCategory category = dishCategoryService.getById(dish.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }
        vo.setImage(minioStorageService.resolveAccessUrl(dish.getImage()));
        vo.setThumbnail(minioStorageService.resolveAccessUrl(dish.getThumbnail()));
        vo.setSpecItems(loadDishSpecItems(List.of(dish.getId())).getOrDefault(dish.getId(), List.of()));
        return vo;
    }

    /**
     * 批量转换 Dish 实体为 DishVO
     */
    private List<DishVO> toDishVOList(List<Dish> dishes) {
        Map<Long, List<DishSpecItemVO>> dishSpecItemMap = loadDishSpecItems(dishes.stream().map(Dish::getId).toList());
        return dishes.stream().map(dish -> {
            DishVO vo = BeanUtil.copyProperties(dish, DishVO.class);
            if (dish.getCategoryId() != null) {
                DishCategory category = dishCategoryService.getById(dish.getCategoryId());
                if (category != null) {
                    vo.setCategoryName(category.getName());
                }
            }
            vo.setImage(minioStorageService.resolveAccessUrl(dish.getImage()));
            vo.setThumbnail(minioStorageService.resolveAccessUrl(dish.getThumbnail()));
            vo.setSpecItems(dishSpecItemMap.getOrDefault(dish.getId(), List.of()));
            return vo;
        }).toList();
    }

    /**
     * 保存菜品规格映射
     *
     * @param dishId 菜品ID
     * @param specItems 菜品规格项
     * @author Henfon
     * @date 2026-07-01
     * @description 菜品保存时重建规格映射，支持按菜品覆盖分类默认规格组和可选项
     */
    private void saveDishSpecMappings(Long dishId, List<DishSpecItemDTO> specItems) {
        LambdaQueryWrapper<DishSpecMapping> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(DishSpecMapping::getDishId, dishId);
        dishSpecMappingMapper.delete(deleteWrapper);

        if (specItems == null || specItems.isEmpty()) {
            return;
        }

        for (DishSpecItemDTO item : specItems) {
            if (item.getSpecGroupId() == null || item.getOptionIds() == null || item.getOptionIds().isEmpty()) {
                continue;
            }
            DishSpecMapping mapping = new DishSpecMapping();
            mapping.setDishId(dishId);
            mapping.setSpecGroupId(item.getSpecGroupId());
            mapping.setOptionIds(item.getOptionIds().stream().map(String::valueOf).collect(Collectors.joining(",")));
            dishSpecMappingMapper.insert(mapping);
        }
    }

    /**
     * 批量加载菜品规格项
     *
     * @param dishIds 菜品ID列表
     * @return 菜品规格映射
     * @author Henfon
     * @date 2026-07-01
     * @description 批量装载菜品规格组和选项名称，避免列表页逐条查询
     */
    private Map<Long, List<DishSpecItemVO>> loadDishSpecItems(List<Long> dishIds) {
        if (dishIds == null || dishIds.isEmpty()) {
            return Map.of();
        }

        LambdaQueryWrapper<DishSpecMapping> mappingWrapper = new LambdaQueryWrapper<>();
        mappingWrapper.in(DishSpecMapping::getDishId, dishIds)
                .orderByAsc(DishSpecMapping::getId);
        List<DishSpecMapping> mappings = dishSpecMappingMapper.selectList(mappingWrapper);
        if (mappings.isEmpty()) {
            return Map.of();
        }

        List<Long> groupIds = mappings.stream().map(DishSpecMapping::getSpecGroupId).distinct().toList();
        LambdaQueryWrapper<DishSpecGroup> groupWrapper = new LambdaQueryWrapper<>();
        groupWrapper.in(DishSpecGroup::getId, groupIds);
        List<DishSpecGroup> groups = dishSpecGroupMapper.selectList(groupWrapper);
        Map<Long, String> groupNameMap = groups.stream().collect(Collectors.toMap(DishSpecGroup::getId, DishSpecGroup::getName));

        List<Long> optionIds = mappings.stream()
                .flatMap(item -> StrUtil.splitTrim(item.getOptionIds(), ",").stream())
                .filter(StrUtil::isNotBlank)
                .map(Long::valueOf)
                .distinct()
                .toList();
        Map<Long, String> optionNameMap = new LinkedHashMap<>();
        if (!optionIds.isEmpty()) {
            LambdaQueryWrapper<DishSpecOption> optionWrapper = new LambdaQueryWrapper<>();
            optionWrapper.in(DishSpecOption::getId, optionIds);
            List<DishSpecOption> options = dishSpecOptionMapper.selectList(optionWrapper);
            optionNameMap = options.stream().collect(Collectors.toMap(DishSpecOption::getId, DishSpecOption::getName));
        }

        Map<Long, List<DishSpecItemVO>> result = new LinkedHashMap<>();
        for (DishSpecMapping mapping : mappings) {
            DishSpecItemVO itemVO = new DishSpecItemVO();
            itemVO.setSpecGroupId(mapping.getSpecGroupId());
            itemVO.setSpecGroupName(groupNameMap.get(mapping.getSpecGroupId()));

            List<Long> currentOptionIds = new ArrayList<>();
            List<String> currentOptionNames = new ArrayList<>();
            for (String rawId : StrUtil.splitTrim(mapping.getOptionIds(), ",")) {
                if (!StrUtil.isNumeric(rawId)) {
                    continue;
                }
                Long optionId = Long.valueOf(rawId);
                currentOptionIds.add(optionId);
                String optionName = optionNameMap.get(optionId);
                if (optionName != null) {
                    currentOptionNames.add(optionName);
                }
            }
            itemVO.setOptionIds(currentOptionIds);
            itemVO.setOptionNames(currentOptionNames);
            result.computeIfAbsent(mapping.getDishId(), key -> new ArrayList<>()).add(itemVO);
        }
        return result;
    }
}
