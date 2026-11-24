package gov.mib.aims.backend.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.mib.aims.backend.exception.BaseException;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Шаги сравнения и независимые JSON-снимки функциональных проверок.
 */
public final class FunctionalTestReport {

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private FunctionalTestReport() {
    }

    /** Записывает наблюдаемые значения до выполнения сравнения. */
    public static void equal(String name, Object expected, Object actual) {
        Allure.step(name, step -> {
            step.parameter("Ожидалось", expected);
            step.parameter("Получено", actual);
            var comparison = new LinkedHashMap<String, Object>();
            comparison.put("Ожидалось", expected);
            comparison.put("Получено", actual);
            snapshot("Результат проверки", comparison);
            assertThat(actual).as(name).isEqualTo(expected);
        });
    }

    /** Сериализует значение сразу и возвращает его независимый снимок. */
    public static JsonNode snapshot(String name, Object value) {
        JsonNode tree = JSON.valueToTree(value);
        try {
            attach(name, "application/json", ".json", JSON.writerWithDefaultPrettyPrinter().writeValueAsString(tree));
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Не удалось сохранить снимок " + name, error);
        }
        return tree;
    }

    /** Сохраняет поля ожидаемого исключения, включая отсутствующие значения. */
    public static void exception(BaseException error) {
        var details = new LinkedHashMap<String, Object>();
        details.put("code", error.getCode());
        details.put("message", error.getMessage());
        details.put("humanMessage", error.getHumanMessage());
        snapshot("Фактическое исключение", details);
    }

    /** Добавляет текстовую диагностику к текущему действию или тесту. */
    public static void text(String name, String content) {
        attach(name, "text/plain", ".txt", content);
    }

    private static void attach(String name, String type, String extension, String content) {
        var input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        Allure.getLifecycle().addAttachment(name, type, input, AttachmentOptions.withFileExtension(extension));
    }
}
