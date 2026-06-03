package gov.mib.aims.backend.services;

import java.io.OutputStream;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

/**
 * Обёртка вокруг {@link ObjectMapper}: изолирует работу с исключениями и упрощает сериализацию.
 */
@Service
@RequiredArgsConstructor
public class ObjectMapperWrapper {

    private final ObjectMapper mapper;

    /**
     * Сериализует объект в JSON-строку; при ошибке возвращает {@link String#valueOf(Object)}.
     *
     * @param obj объект
     * @return JSON или строковое представление объекта
     */
    public String writeValueAsString(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }

    /**
     * Сериализует объект в JSON-строку; при ошибке бросает {@link IllegalStateException}.
     *
     * @param obj объект
     * @return JSON
     */
    public String writeValueAsStringOrThrow(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize to JSON", e);
        }
    }

    /**
     * Записывает объект в поток как JSON.
     *
     * @param out поток вывода
     * @param value объект
     */
    @SneakyThrows
    public void writeValue(OutputStream out, Object value) {
        mapper.writeValue(out, value);
    }

    /**
     * Десериализует объект из JSON-строки.
     *
     * @param data JSON
     * @param type тип
     * @return объект
     */
    @SneakyThrows
    public <T> T readValue(String data, TypeReference<T> type) {
        return mapper.readValue(data, type);
    }

    /**
     * Десериализует объект из JSON-строки.
     *
     * @param data JSON
     * @param cls класс
     * @return объект
     */
    @SneakyThrows
    public <T> T readValue(String data, Class<T> cls) {
        return mapper.readValue(data, cls);
    }

    /**
     * Десериализует JSON-объект в map.
     *
     * @param data JSON
     * @return map
     */
    @SneakyThrows
    public Map<String, Object> readMap(String data) {
        return readValue(data, new TypeReference<>() {
        });
    }

    /**
     * Десериализует JSON-дерево из строки.
     *
     * @param data JSON
     * @return дерево
     */
    @SneakyThrows
    public JsonNode readTree(String data) {
        return mapper.readTree(data);
    }
}
