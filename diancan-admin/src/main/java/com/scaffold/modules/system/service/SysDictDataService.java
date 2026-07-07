package com.scaffold.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.modules.system.dto.DictDataCreateDTO;
import com.scaffold.modules.system.dto.DictDataUpdateDTO;
import com.scaffold.modules.system.entity.SysDictData;
import com.scaffold.modules.system.vo.DictDataVO;

import java.util.List;

/**
 * 字典数据服务接口
 *
 * @author Henfon
 */
public interface SysDictDataService extends IService<SysDictData> {

    /**
     * 创建字典数据
     *
     * @param dto 创建参数
     */
    void createDictData(DictDataCreateDTO dto);

    /**
     * 更新字典数据
     *
     * @param dto 更新参数
     */
    void updateDictData(DictDataUpdateDTO dto);

    /**
     * 删除字典数据
     *
     * @param dictDataId 字典数据ID
     */
    void deleteDictData(Long dictDataId);

    /**
     * 根据字典类型ID查询数据列表
     *
     * @param typeId 字典类型ID
     * @return 字典数据列表
     */
    List<DictDataVO> getByTypeId(Long typeId);

    /**
     * 根据字典类型编码查询数据列表
     *
     * @param typeCode 字典类型编码
     * @return 字典数据列表
     */
    List<DictDataVO> getByTypeCode(String typeCode);

    /**
     * 刷新字典缓存
     */
    void refreshCache();
}
