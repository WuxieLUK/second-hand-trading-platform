package org.example.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.example.order.interceptor.LoginInterceptor;
import org.example.order.interceptor.RequestTraceInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OrderWebConfig implements WebMvcConfigurer {

    private final RequestTraceInterceptor requestTraceInterceptor;
    private final LoginInterceptor loginInterceptor;

    public OrderWebConfig(RequestTraceInterceptor requestTraceInterceptor, LoginInterceptor loginInterceptor) {
        this.requestTraceInterceptor = requestTraceInterceptor;
        this.loginInterceptor = loginInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestTraceInterceptor);
        registry.addInterceptor(loginInterceptor)
                .excludePathPatterns(
                        "/order/internal/**",
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
    public OpenAPI orderOpenApi() {
        return new OpenAPI().info(new Info().title("service-order").version("2.0.0"));
    }
}
