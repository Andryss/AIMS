package gov.mib.aims.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки файлового хранилища.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "aims.storage")
public class StorageProperties {

    private String basePath = System.getProperty("user.home") + "/.aims/storage";
}
