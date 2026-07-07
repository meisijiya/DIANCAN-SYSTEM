package com.scaffold.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.common.result.PageResult;
import com.scaffold.modules.system.dto.ConfigCreateDTO;
import com.scaffold.modules.system.dto.ConfigQueryDTO;
import com.scaffold.modules.system.dto.ConfigUpdateDTO;
import com.scaffold.modules.system.entity.SysConfig;
import com.scaffold.modules.system.vo.ConfigVO;

/**
 * 系统配置服务接口
 *
 * @author Henfon
 */
public interface SysConfigService extends IService<SysConfig> {

    /**
     * 创建配置
     *
     * @param dto 创建参数
     */
    void createConfig(ConfigCreateDTO dto);

    /**
     * 更新配置
     *
     * @param dto 更新参数
     */
    void updateConfig(ConfigUpdateDTO dto);

    /**
     * 删除配置
     *
     * @param configId 配置ID
     */
    void deleteConfig(Long configId);

    /**
     * 分页查询配置
     *
     * @param dto 查询参数
     * @return 分页结果
     */
    PageResult<ConfigVO> pageList(ConfigQueryDTO dto);

    /**
     * 根据配置键获取配置值
     *
     * @param configKey 配置键
     * @return 配置值
     */
    String getConfigValue(String configKey);

    /**
     * 按配置键保存配置值
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 配置存在时更新，不存在时自动创建，并同步刷新缓存。
     * @param configKey 配置键
     * @param configValue 配置值
     * @param name 配置名称
     * @param remark 配置备注
     */
    void saveConfigValue(String configKey, String configValue, String name, String remark);

    /**
     * 刷新配置缓存
     */
    void refreshCache();
}
