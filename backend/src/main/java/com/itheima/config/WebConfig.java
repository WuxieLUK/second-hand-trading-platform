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

/**
 * Web MVC配置类：拦截器、跨域、静态资源映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 抽离排除路径为常量，便于维护
    private static final String[] EXCLUDE_PATHS = {
            // 业务接口
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

    // 构造器注入（推荐），避免@Autowired的潜在问题
    private final LoginInterceptor loginInterceptor;
    public WebConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .excludePathPatterns(EXCLUDE_PATHS);
    }

    /**
     * 配置跨域（适配Spring 5.3+，解决CORS报错）
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 生产环境建议配置为具体域名，如https://your-frontend.com
                .allowedOriginPatterns("**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Type") // 只暴露需要的响应头，而非全部
                .allowCredentials(true)
                .maxAge(3600);
    }



    /**
     * 自定义API文档的基本信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // API文档标题、描述、版本
                .info(new Info()
                        .title("Spring Boot 3 API文档")
                        .description("这是基于SpringDoc OpenAPI的接口文档")
                        .version("v1.0.0")
                        // 联系人信息（可选）
                        .contact(new Contact()
                                .name("开发者")
                                .email("developer@example.com")));
    }

    /**
     * 静态资源映射（可选，建议通过配置文件读取路径）
     * */
    /*@Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 从配置文件读取路径，示例：@Value("${file.upload.path}") private String uploadPath;
        String uploadPath = "file:" + System.getProperty("user.dir") + "/upload/pic/";
        registry.addResourceHandler("/pic/**")
                .addResourceLocations(uploadPath);
    }*/
}