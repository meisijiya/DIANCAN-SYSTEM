package com.scaffold.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 接口文档配置
 *
 * @author Henfon
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("后端脚手架 API 文档")
                        .description("基于 SpringBoot3 + MyBatis-Plus + Sa-Token 的后端脚手架")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("scaffold")
                                .email("scaffold@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .schemaRequirement("Authorization", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .name("Authorization")
                        .in(SecurityScheme.In.HEADER)
                        .description("Sa-Token 认证"))
                .addSecurityItem(new SecurityRequirement().addList("Authorization"));
    }
}
