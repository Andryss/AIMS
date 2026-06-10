package gov.mib.aims.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import java.util.List;

/**
 * Раздача фронтенд-артефактов из {@code classpath:/static/} по URL {@code /public/**}
 * с SPA fallback на {@code index.html} для клиентских маршрутов.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/public/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaFallbackResourceResolver());
    }

    /**
     * Отдаёт {@code index.html}, если запрошенный путь не соответствует статическому файлу.
     */
    private static final class SpaFallbackResourceResolver extends PathResourceResolver {

        @Override
        protected Resource resolveResourceInternal(
                HttpServletRequest request,
                String requestPath,
                List<? extends Resource> locations,
                ResourceResolverChain chain
        ) {
            Resource resource = super.resolveResourceInternal(request, requestPath, locations, chain);
            if (resource != null) {
                return resource;
            }
            return super.resolveResourceInternal(request, "index.html", locations, chain);
        }
    }
}
