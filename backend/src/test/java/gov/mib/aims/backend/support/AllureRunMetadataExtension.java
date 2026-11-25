package gov.mib.aims.backend.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.UUID;

/**
 * Записывает метаданные Allure один раз для всех тестовых классов в прогоне JUnit.
 */
public class AllureRunMetadataExtension implements BeforeAllCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(AllureRunMetadataExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) {
        context.getRoot().getStore(NAMESPACE)
                .getOrComputeIfAbsent(RunMetadata.class, key -> new RunMetadata(), RunMetadata.class);
    }

    private static final class RunMetadata implements ExtensionContext.Store.CloseableResource {

        private static final ZoneId REPORT_ZONE = ZoneId.of("Europe/Moscow");
        private final Properties environment = new Properties();
        private final Path results = Path.of(System.getProperty("allure.results.directory"));
        private final ZonedDateTime startedAt = ZonedDateTime.now(REPORT_ZONE);

        private RunMetadata() {
            String runId = UUID.randomUUID().toString();
            environment.setProperty("Application.version", System.getProperty("aims.test.version", "unknown"));
            String revision = System.getProperty("aims.test.revision", "unknown");
            if (!revision.equals("unknown")) {
                environment.setProperty("Git.commit", revision);
            }
            environment.setProperty("JDK", System.getProperty("java.runtime.version"));
            environment.setProperty("JUnit.version", System.getProperty("aims.test.junit", "unknown"));
            String mockitoVersion = System.getProperty("aims.test.mockito");
            if (mockitoVersion != null) {
                environment.setProperty("Mockito.version", mockitoVersion);
            }
            environment.setProperty("Allure.Java.version", System.getProperty("aims.test.allure", "unknown"));
            environment.setProperty("OS", System.getProperty("os.name") + " " + System.getProperty("os.version")
                    + " " + System.getProperty("os.arch"));
            environment.setProperty("Run.id", runId);
            environment.setProperty("Run.started_at", startedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            environment.setProperty("Run.timezone", REPORT_ZONE.getId());
            LinkedHashMap<String, Object> executor = new LinkedHashMap<>();
            executor.put("name", "Maven / JUnit Jupiter");
            executor.put("buildName", runId);
            executor.put("buildOrder", startedAt.toInstant().toEpochMilli());
            executor.put("reportName", System.getProperty("aims.test.report-name", "Тестирование AIMS"));
            try {
                Files.createDirectories(results);
                writeEnvironment();
                new ObjectMapper().writerWithDefaultPrettyPrinter()
                        .writeValue(results.resolve("executor.json").toFile(), executor);
            } catch (IOException error) {
                throw new UncheckedIOException("Не удалось записать метаданные тестового прогона", error);
            }
        }

        @Override
        public void close() {
            ZonedDateTime finishedAt = ZonedDateTime.now(REPORT_ZONE);
            environment.setProperty("Run.finished_at", finishedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            environment.setProperty("Run.duration_ms",
                    Long.toString(Duration.between(startedAt, finishedAt).toMillis()));
            try {
                writeEnvironment();
            } catch (IOException error) {
                throw new UncheckedIOException("Не удалось завершить метаданные тестового прогона", error);
            }
        }

        private void writeEnvironment() throws IOException {
            String prefix = "aims.test.environment.";
            System.getProperties().stringPropertyNames().stream().filter(key -> key.startsWith(prefix))
                    .forEach(key -> environment.setProperty(key.substring(prefix.length()), System.getProperty(key)));
            Path destination = results.resolve("environment.properties");
            try (Writer output = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
                environment.store(output, "AIMS test run");
            }
        }
    }
}
