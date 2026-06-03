package gov.mib.aims.backend.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Настройки файлового хранилища.
 */
@Configuration
@Data
@Validated
@ConfigurationProperties(prefix = "aims.storage")
public class StorageProperties {

    /**
     * Корневая директория для сохранения загруженных файлов.
     */
    @NotBlank
    private String basePath = System.getProperty("user.home") + "/.aims/storage";
}
