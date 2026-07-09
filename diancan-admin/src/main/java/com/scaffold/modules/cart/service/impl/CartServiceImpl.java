package com.scaffold.modules.cart.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.cart.dto.CartItemDTO;
import com.scaffold.modules.cart.service.CartService;
import com.scaffold.modules.cart.vo.CartItemVO;
import com.scaffold.modules.cart.vo.CartVO;
import com.scaffold.modules.dish.entity.Dish;
import com.scaffold.modules.dish.service.DishService;
import com.scaffold.modules.table.service.DiningTableService;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 购物车服务实现
 * <p>
 * 购物车数据存储在 Redis 中，key: cart:{openid}:{tableId}:{sessionCode}，Hash 结构，TTL 2小时。
 * Hash field: dishId（字符串），Hash value: CartItemVO 的 JSON。
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final DishService dishService;
    private final DiningTableService diningTableService;

    private static final long CART_TTL_HOURS = 2;

    @Override
    public CartVO addItem(String openid, Long tableId, CartItemDTO dto) {
        // 校验菜品是否存在、上架且未售罄
        Dish dish = dishService.getById(dto.getDishId());
        if (dish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (dish.getStatus() == null || dish.getStatus() != 1) {
            throw new BusinessException(ResultCode.DISH_OFF_SHELF);
        }
        if (dish.getSoldOut() != null && dish.getSoldOut() == 1) {
            throw new BusinessException(ResultCode.DISH_SOLD_OUT);
        }

        String cartKey = buildCartKey(openid, tableId);
        String field = dto.getDishId().toString();

        // 查看购物车中是否已有该菜品
        CartItemVO existingItem = getCartItem(cartKey, field);
        if (existingItem != null) {
            // 已存在则累加数量
            existingItem.setQuantity(existingItem.getQuantity() + dto.getQuantity());
            existingItem.setAmount(existingItem.getPrice().multiply(BigDecimal.valueOf(existingItem.getQuantity())));
            if (dto.getRemark() != null) {
                existingItem.setRemark(dto.getRemark());
            }
            saveCartItem(cartKey, field, existingItem);
        } else {
            // 新增购物车项
            CartItemVO item = new CartItemVO();
            item.setDishId(dish.getId());
            item.setDishName(dish.getName());
            item.setDishImage(pickDishImage(dish));
            item.setPrice(dish.getPrice());
            item.setQuantity(dto.getQuantity());
            item.setRemark(dto.getRemark());
            item.setAmount(dish.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity())));
            saveCartItem(cartKey, field, item);
        }

        // 刷新 TTL
        redisTemplate.expire(cartKey, CART_TTL_HOURS, TimeUnit.HOURS);
        log.info("购物车添加菜品: openid={}, tableId={}, dishId={}, quantity={}", openid, tableId, dto.getDishId(), dto.getQuantity());
        return getCart(openid, tableId);
    }

    @Override
    public CartVO updateItemQuantity(String openid, Long tableId, Long dishId, int quantity) {
        String cartKey = buildCartKey(openid, tableId);
        String field = dishId.toString();

        if (quantity <= 0) {
            // 数量为0或负数，移除该项
            redisTemplate.opsForHash().delete(cartKey, field);
            log.info("购物车移除菜品（数量为0）: openid={}, tableId={}, dishId={}", openid, tableId, dishId);
        } else {
            CartItemVO item = getCartItem(cartKey, field);
            if (item == null) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
            item.setQuantity(quantity);
            item.setAmount(item.getPrice().multiply(BigDecimal.valueOf(quantity)));
            saveCartItem(cartKey, field, item);
        }

        // 刷新 TTL
        redisTemplate.expire(cartKey, CART_TTL_HOURS, TimeUnit.HOURS);
        return getCart(openid, tableId);
    }

    @Override
    public CartVO updateItemRemark(String openid, Long tableId, Long dishId, String remark) {
        String cartKey = buildCartKey(openid, tableId);
        String field = dishId.toString();

        CartItemVO item = getCartItem(cartKey, field);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        item.setRemark(remark);
        saveCartItem(cartKey, field, item);

        // 刷新 TTL
        redisTemplate.expire(cartKey, CART_TTL_HOURS, TimeUnit.HOURS);
        return getCart(openid, tableId);
    }

    @Override
    public CartVO getCart(String openid, Long tableId) {
        String cartKey = buildCartKey(openid, tableId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cartKey);

        List<CartItemVO> items = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            CartItemVO item = deserializeCartItem(entry.getValue());
            if (item != null) {
                items.add(item);
            }
        }

        // 计算总数和总价
        int totalCount = items.stream().mapToInt(CartItemVO::getQuantity).sum();
        BigDecimal totalPrice = items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartVO cartVO = new CartVO();
        cartVO.setTableId(tableId);
        cartVO.setItems(items);
        cartVO.setTotalCount(totalCount);
        cartVO.setTotalPrice(totalPrice);
        return cartVO;
    }

    @Override
    public void clearCart(String openid, Long tableId) {
        String cartKey = buildCartKey(openid, tableId);
        redisTemplate.delete(cartKey);
        log.info("购物车已清空: openid={}, tableId={}", openid, tableId);
    }

    @Override
    public void removeItem(String openid, Long tableId, Long dishId) {
        String cartKey = buildCartKey(openid, tableId);
        redisTemplate.opsForHash().delete(cartKey, dishId.toString());
        log.info("购物车移除菜品: openid={}, tableId={}, dishId={}", openid, tableId, dishId);
    }

    // ==================== 私有方法 ====================

    private String buildCartKey(String openid, Long tableId) {
        String sessionCode = resolveCartSessionCode(tableId);
        return "cart:" + openid + ":" + tableId + ":" + sessionCode;
    }

    /**
     * 从 Redis Hash 中获取购物车项
     */
    private CartItemVO getCartItem(String cartKey, String field) {
        Object value = redisTemplate.opsForHash().get(cartKey, field);
        return deserializeCartItem(value);
    }

    /**
     * 保存购物车项到 Redis Hash
     */
    private void saveCartItem(String cartKey, String field, CartItemVO item) {
        try {
            String json = objectMapper.writeValueAsString(item);
            redisTemplate.opsForHash().put(cartKey, field, json);
        } catch (JsonProcessingException e) {
            log.error("购物车项序列化失败", e);
            throw new BusinessException("购物车数据处理失败");
        }
    }

    /**
     * 反序列化购物车项
     */
    private CartItemVO deserializeCartItem(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof String json) {
                return objectMapper.readValue(json, CartItemVO.class);
            }
            // 兼容 RedisTemplate 直接反序列化的情况
            return objectMapper.convertValue(value, CartItemVO.class);
        } catch (Exception e) {
            log.error("购物车项反序列化失败", e);
            return null;
        }
    }

    private String pickDishImage(Dish dish) {
        if (dish == null) {
            return null;
        }
        if (hasText(dish.getImage())) {
            return dish.getImage();
        }
        return hasText(dish.getThumbnail()) ? dish.getThumbnail() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 解析购物车所属桌次
     *
     * @author Henfon
     * @date 2026-07-10
     * @description 优先读取桌台当前活跃桌次，避免同桌不同批客人的购物车互相串读。
     * @param tableId 桌台ID
     * @return 桌次编码
     */
    private String resolveCartSessionCode(Long tableId) {
        String sessionCode = diningTableService.getActiveSessionCode(tableId);
        return StrUtil.blankToDefault(sessionCode, "default");
    }
}
