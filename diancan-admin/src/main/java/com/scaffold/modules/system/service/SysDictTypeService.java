package com.scaffold.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.common.result.PageResult;
import com.scaffold.modules.system.dto.DictTypeCreateDTO;
import com.scaffold.modules.system.dto.DictTypeQueryDTO;
import com.scaffold.modules.system.dto.DictTypeUpdateDTO;
import com.scaffold.modules.system.entity.SysDictType;
import com.scaffold.modules.system.vo.DictTypeVO;

import java.util.List;

/**
 * 字典类型服务接口
 *
 * @author Henfon
 */
public interface SysDictTypeService extends IService<SysDictType> {

    /**
     * 创建字典类型
     *
     * @param dto 创建参数
     */
    void createDictType(DictTypeCreateDTO dto);

    /**
     * 更新字典类型
     *
     * @param dto 更新参数
     */
    void updateDictType(DictTypeUpdateDTO dto);

    /**
     * 删除字典类型
     *
     * @param dictTypeId 字典类型ID
     */
    void deleteDictType(Long dictTypeId);

    /**
     * 分页查询字典类型
     *
     * @param dto 查询参数
     * @return 分页结果
     */
    PageResult<DictTypeVO> pageList(DictTypeQueryDTO dto);

    /**
     * 获取所有字典类型列表
     *
     * @return 字典类型列表
     */
    List<DictTypeVO> listAll();
}
