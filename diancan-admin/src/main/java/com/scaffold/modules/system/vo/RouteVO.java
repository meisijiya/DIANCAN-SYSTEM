package com.scaffold.modules.system.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 路由VO（适配Soybean Admin ElegantConstRoute格式）
 *
 * @author Henfon
 */
@Data
public class RouteVO {
    
    /** 路由名称 */
    private String name;
    
    /** 路由路径 */
    private String path;
    
    /** 组件 */
    private String component;
    
    /** 路由元信息 */
    private RouteMeta meta;
    
    /** 子路由 */
    private List<RouteVO> children;
    
    /** 路由参数传递 */
    private Boolean props;
    
    /** 路由ID */
    private String id;
    
    @Data
    public static class RouteMeta {
        /** 标题 */
        private String title;
        
        /** 国际化标题key */
        private String i18nKey;
        
        /** 图标 */
        private String icon;
        
        /** 本地图标 */
        private String localIcon;
        
        /** 排序 */
        private Integer order;
        
        /** 是否缓存 */
        private Boolean keepAlive;
        
        /** 是否隐藏 */
        private Boolean hideInMenu;
        
        /** 是否常量路由（不需要登录） */
        private Boolean constant;
        
        /** 外链地址 */
        private String href;
        
        /** 是否固定在tab */
        private Boolean fixedIndexInTab;
        
        /** 激活的菜单key */
        private String activeMenu;
        
        /** 是否支持多tab */
        private Boolean multiTab;
        
        /** 查询参数 */
        private List<Map<String, String>> query;
    }
}
