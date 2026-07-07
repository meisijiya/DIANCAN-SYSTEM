package com.scaffold.modules.system.vo;

import lombok.Data;

import java.util.List;

/**
 * 用户路由VO
 *
 * @author Henfon
 */
@Data
public class UserRouteVO {
    
    /** 路由列表 */
    private List<RouteVO> routes;
    
    /** 首页路由名称 */
    private String home;
}
