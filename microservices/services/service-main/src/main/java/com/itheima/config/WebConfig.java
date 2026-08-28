package com.itheima.config;

import com.itheima.interceptors.LoginInterceptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String[] EXCLUDE_PATHS = {
            "/user/login",
            "/user/register",
            "/shopcategory/list",
            "/comment/commentList/{articleId}",
            "/comment/alllist",
            "/goods/goodsopenlist",
            "/goods/detail/{id}",
            "/goods/findSellerById/{id}",
            "/goods/findSellerByUserId/{id}",
            "/comment/commentallList/{userId}",
            "/goods/seller/alllist",
            "/swagger-ui/**",
            "/goods/rag/search",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/goods/recommend/byKeyword",
    };

    private final LoginInterceptor loginInterceptor;

    public WebConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .excludePathPatterns(EXCLUDE_PATHS);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot 3 API文档")
                        .description("基于SpringDoc OpenAPI的接口文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("开发者")
                                .email("developer@example.com")));
    }
}
