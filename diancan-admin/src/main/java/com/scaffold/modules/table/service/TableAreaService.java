package com.scaffold.modules.table.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.modules.table.dto.TableAreaCreateDTO;
import com.scaffold.modules.table.dto.TableAreaUpdateDTO;
import com.scaffold.modules.table.entity.TableArea;
import com.scaffold.modules.table.vo.TableAreaVO;

import java.util.List;

/**
 * 桌台区域服务接口
 *
 * @author Henfon
 */
public interface TableAreaService extends IService<TableArea> {

    /**
     * 获取全部区域列表
     *
     * @author Henfon
     * @date 2026-07-02
     * @description 返回桌台区域主数据，供管理页和筛选器统一使用。
     * @return 区域列表
     */
    List<TableAreaVO> listAll();

    /**
     * 获取启用区域列表
     *
     * @author Henfon
     * @date 2026-07-02
     * @description 仅返回启用中的区域，供桌台选择器使用。
     * @return 区域列表
     */
    List<TableAreaVO> listEnabled();

    /**
     * 创建桌台区域
     *
     * @author Henfon
     * @date 2026-07-02
     * @description 新增桌台区域主数据。
     * @param dto 创建参数
     */
    void createArea(TableAreaCreateDTO dto);

    /**
     * 更新桌台区域
     *
     * @author Henfon
     * @date 2026-07-02
     * @description 更新区域资料，并同步冗余到桌台表的区域名称。
     * @param dto 更新参数
     */
    void updateArea(TableAreaUpdateDTO dto);

    /**
     * 删除桌台区域
     *
     * @author Henfon
     * @date 2026-07-02
     * @description 删除未被桌台引用的区域。
     * @param id 区域ID
     */
    void deleteArea(Long id);
}
