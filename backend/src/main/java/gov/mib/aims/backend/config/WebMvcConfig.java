package gov.mib.aims.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Раздача фронтенд-артефактов из {@code classpath:/static/} по URL {@code /public/**}.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/public/**")
                .addResourceLocations(
                        "classpath:/static/"
                );
    }
}
