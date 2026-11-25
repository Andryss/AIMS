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
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Записывает шаги проверок, ожидаемые и фактические значения и вложения тестов в Allure.
 */
public final class AllureTestDataWriter {

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private AllureTestDataWriter() {
    }

    /** Записывает наблюдаемые значения до выполнения сравнения. */
    public static void equal(String name, Object expected, Object actual) {
        equal(name, expected, () -> actual);
    }

    /** Читает фактическое значение внутри шага, сохраняя контекст ошибки чтения. */
    public static void equal(String name, Object expected, Supplier<?> readActual) {
        Allure.step(name, step -> {
            step.parameter("Ожидалось", expected);
            Object actual = readActual == null ? null : readActual.get();
            step.parameter("Получено", actual);
            LinkedHashMap<String, Object> comparison = new LinkedHashMap<>();
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
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("code", error.getCode());
        details.put("message", error.getMessage());
        details.put("humanMessage", error.getHumanMessage());
        snapshot("Фактическое исключение", details);
    }

    /** Добавляет текстовую диагностику к текущему действию или тесту. */
    public static void text(String name, String content) {
        attach(name, "text/plain", ".txt", content);
    }

    /** Прикрепляет PNG к текущему шагу или тесту. */
    public static void png(String name, byte[] content) {
        Allure.getLifecycle().addAttachment(name, "image/png", new ByteArrayInputStream(content),
                AttachmentOptions.withFileExtension(".png"));
    }

    private static void attach(String name, String type, String extension, String content) {
        ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        Allure.getLifecycle().addAttachment(name, type, input, AttachmentOptions.withFileExtension(extension));
    }
}
