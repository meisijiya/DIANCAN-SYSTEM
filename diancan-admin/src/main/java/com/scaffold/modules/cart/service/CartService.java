package com.scaffold.modules.cart.service;

import com.scaffold.modules.cart.dto.CartItemDTO;
import com.scaffold.modules.cart.vo.CartVO;

/**
 * 购物车服务接口
 *
 * @author Henfon
 */
public interface CartService {

    /**
     * 添加菜品到购物车，校验菜品是否上架且未售罄
     *
     * @param openid  用户openid
     * @param tableId 桌台ID
     * @param dto     购物车项参数
     * @return 购物车信息
     */
    CartVO addItem(String openid, Long tableId, CartItemDTO dto);

    /**
     * 修改购物车项数量，数量为0时自动移除
     *
     * @param openid   用户openid
     * @param tableId  桌台ID
     * @param dishId   菜品ID
     * @param quantity 新数量
     * @return 购物车信息
     */
    CartVO updateItemQuantity(String openid, Long tableId, Long dishId, int quantity);

    /**
     * 修改购物车项备注
     *
     * @param openid  用户openid
     * @param tableId 桌台ID
     * @param dishId  菜品ID
     * @param remark  备注
     * @return 购物车信息
     */
    CartVO updateItemRemark(String openid, Long tableId, Long dishId, String remark);

    /**
     * 获取购物车，返回菜品总数和总价
     *
     * @param openid  用户openid
     * @param tableId 桌台ID
     * @return 购物车信息
     */
    CartVO getCart(String openid, Long tableId);

    /**
     * 清空购物车
     *
     * @param openid  用户openid
     * @param tableId 桌台ID
     */
    void clearCart(String openid, Long tableId);

    /**
     * 移除单个购物车项
     *
     * @param openid  用户openid
     * @param tableId 桌台ID
     * @param dishId  菜品ID
     */
    void removeItem(String openid, Long tableId, Long dishId);
}
