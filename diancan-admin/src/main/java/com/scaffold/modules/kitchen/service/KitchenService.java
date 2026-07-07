package com.scaffold.modules.kitchen.service;

import com.scaffold.modules.kitchen.vo.KitchenTaskVO;

import java.util.List;

/**
 * 后厨任务服务接口
 *
 * @author Henfon
 */
public interface KitchenService {

    /**
     * 获取待制作/制作中任务列表（按下单时间升序排列）
     *
     * @return 任务列表
     */
    List<KitchenTaskVO> getTaskList();

    /**
     * 接单：将订单项状态从"待制作"(0)变更为"制作中"(1)
     *
     * @param itemId 订单项ID
     */
    void acceptTask(Long itemId);

    /**
     * 自动接单开关是否开启
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 读取后厨自动接单配置，控制新堂食订单是否自动进入制作中。
     * @return 是否开启自动接单
     */
    boolean isAutoAcceptEnabled();

    /**
     * 设置自动接单开关
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 持久化后厨自动接单配置，并立即刷新缓存。
     * @param enabled 是否开启
     */
    void updateAutoAcceptEnabled(boolean enabled);

    /**
     * 自动接单指定订单项
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 将新生成的待制作订单项批量推进到制作中，供自动接单场景使用。
     * @param itemIds 订单项ID列表
     */
    void autoAcceptTasks(List<Long> itemIds);

    /**
     * 划单：将订单项状态从"制作中"(1)变更为"已完成"(2)
     * 当订单所有订单项均为"已完成"时，推送 ALL_COMPLETED 事件
     *
     * @param itemId 订单项ID
     */
    void completeTask(Long itemId);
}
