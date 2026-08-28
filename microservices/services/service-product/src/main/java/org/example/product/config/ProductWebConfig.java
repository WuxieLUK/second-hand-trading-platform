package org.example.product.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.example.product.interceptor.LoginInterceptor;
import org.example.product.interceptor.RequestTraceInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ProductWebConfig implements WebMvcConfigurer {

    private final RequestTraceInterceptor requestTraceInterceptor;
    private final LoginInterceptor loginInterceptor;

    public ProductWebConfig(RequestTraceInterceptor requestTraceInterceptor, LoginInterceptor loginInterceptor) {
        this.requestTraceInterceptor = requestTraceInterceptor;
        this.loginInterceptor = loginInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestTraceInterceptor);
        registry.addInterceptor(loginInterceptor)
                .excludePathPatterns(
                        "/goods/goodsopenlist",
                        "/goods/detail/**",
                        "/goods/findSellerByUserId/**",
                        "/goods/seller/alllist",
                        "/goods/internal/**",
                        "/internal/goods/**",
                        "/ai/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public OpenAPI productOpenApi() {
        return new OpenAPI().info(new Info().title("service-product").version("2.0.0"));
    }
}
