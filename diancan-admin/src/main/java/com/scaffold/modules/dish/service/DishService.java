package com.scaffold.modules.dish.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.modules.dish.dto.DishCreateDTO;
import com.scaffold.modules.dish.dto.DishQueryDTO;
import com.scaffold.modules.dish.dto.DishUpdateDTO;
import com.scaffold.modules.dish.entity.Dish;
import com.scaffold.modules.dish.vo.DishListVO;
import com.scaffold.modules.dish.vo.DishVO;

import java.util.List;
import java.util.Map;

/**
 * 菜品服务接口
 *
 * @author Henfon
 */
public interface DishService extends IService<Dish> {

    /**
     * 获取上架且未售罄的菜品列表（优先从 Redis 缓存读取），按分类分组
     *
     * @return 按分类ID分组的菜品列表
     */
    Map<Long, List<DishListVO>> listOnSaleDishes();

    /**
     * 关键词搜索菜品（名称或配料模糊匹配）
     *
     * @param keyword 搜索关键词
     * @return 匹配的菜品列表
     */
    List<DishVO> searchDishes(String keyword);

    /**
     * 获取菜品详情
     *
     * @param id 菜品ID
     * @return 菜品详情
     */
    DishVO getDishDetail(Long id);

    /**
     * 创建菜品
     *
     * @param dto 创建参数
     */
    void createDish(DishCreateDTO dto);

    /**
     * 更新菜品
     *
     * @param dto 更新参数
     */
    void updateDish(DishUpdateDTO dto);

    /**
     * 更新菜品状态（上架/下架）
     *
     * @param id     菜品ID
     * @param status 状态（0下架 1上架）
     */
    void updateDishStatus(Long id, Integer status);

    /**
     * 估清/取消估清
     *
     * @param dishId  菜品ID
     * @param soldOut 是否售罄（0否 1是）
     */
    void markSoldOut(Long dishId, Integer soldOut);

    /**
     * 库存扣减（下单时调用）
     *
     * @param dishId   菜品ID
     * @param quantity 扣减数量
     * @return 是否扣减成功
     */
    boolean deductStock(Long dishId, int quantity);

    /**
     * 管理端分页查询菜品列表（支持按分类、状态筛选）
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<DishVO> listDishesForAdmin(int pageNum, int pageSize, DishQueryDTO queryDTO);
}
