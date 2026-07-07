package com.scaffold.modules.dish.service;

import com.scaffold.modules.dish.dto.DishSpecGroupCreateDTO;
import com.scaffold.modules.dish.dto.DishSpecGroupUpdateDTO;
import com.scaffold.modules.dish.vo.DishSpecGroupVO;

import java.util.List;

/**
 * 菜品规格组服务接口
 *
 * @author Henfon
 */
public interface DishSpecGroupService {

    /**
     * 查询规格组列表
     *
     * @return 规格组列表
     * @author Henfon
     * @date 2026-07-01
     * @description 返回规格组及其选项，供分类和菜品配置复用
     */
    List<DishSpecGroupVO> listAll();

    /**
     * 创建规格组
     *
     * @param dto 创建参数
     * @author Henfon
     * @date 2026-07-01
     * @description 新增规格组并一次性保存规格选项
     */
    void createGroup(DishSpecGroupCreateDTO dto);

    /**
     * 更新规格组
     *
     * @param dto 更新参数
     * @author Henfon
     * @date 2026-07-01
     * @description 更新规格组基础信息并重建规格选项集合
     */
    void updateGroup(DishSpecGroupUpdateDTO dto);

    /**
     * 删除规格组
     *
     * @param id 规格组ID
     * @author Henfon
     * @date 2026-07-01
     * @description 删除前会校验是否仍被分类或菜品引用
     */
    void deleteGroup(Long id);
}
