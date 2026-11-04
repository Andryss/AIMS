package gov.mib.aims.backend.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Настройки локального распознавания и ограничений ресурсов.
 */
@Component
@ConfigurationProperties(prefix = "aims.alien-classifier")
@Validated
@Getter
@Setter
public class AlienClassifierProperties {

    private static final int DEFAULT_THREADS = 2;
    private static final int MAX_THREADS = 8;
    private static final int DEFAULT_CONCURRENT_REQUESTS = 1;
    private static final int MAX_CONCURRENT_REQUESTS = 4;
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final int DEFAULT_MAX_PIXELS = 4_000_000;
    private static final int MAX_PIXELS = 16_000_000;

    private Resource modelPath = new ClassPathResource("alien-classifier/model.onnx");
    @Min(1)
    @Max(MAX_THREADS)
    private int threads = DEFAULT_THREADS;
    @Min(1)
    @Max(MAX_CONCURRENT_REQUESTS)
    private int maxConcurrentRequests = DEFAULT_CONCURRENT_REQUESTS;
    @Min(1)
    @Max(MAX_IMAGE_BYTES)
    private int maxImageBytes = MAX_IMAGE_BYTES;
    @Min(1)
    @Max(MAX_PIXELS)
    private int maxPixels = DEFAULT_MAX_PIXELS;
}
