package gov.mib.aims.backend.config.dbqueue;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Настройки db-queue.
 */
@Configuration
@Data
@Validated
@ConfigurationProperties("db-queue")
public class DbQueueProperties {

    /**
     * Если true — фоновая обработка очередей включена.
     */
    private boolean processingEnabled = true;

    /**
     * Название таблицы БД очередей.
     */
    @NotBlank
    private String tableName;

    /**
     * Идентификатор шарда.
     */
    @NotBlank
    private String shardId;
}
