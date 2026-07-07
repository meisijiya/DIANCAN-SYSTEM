package com.scaffold;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 点餐管理系统启动类
 *
 * @author Henfon
 */
@SpringBootApplication
@MapperScan("com.scaffold.modules.*.mapper")
@EnableAsync
public class DiancanAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiancanAdminApplication.class, args);
    }

}
