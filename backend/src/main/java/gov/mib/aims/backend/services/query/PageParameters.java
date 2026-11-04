package gov.mib.aims.backend.services.query;

import gov.mib.aims.backend.exception.Errors;
import org.springframework.data.domain.PageRequest;

/**
 * Единые ограничения пагинации; значения по умолчанию задаются контрактом endpoint.
 */
public record PageParameters(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageParameters {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw Errors.validationError("Номер страницы должен быть неотрицательным, размер — от 1 до 100");
        }
    }

    /**
     * Создаёт параметры со стандартным размером страницы.
     */
    public static PageParameters of(Integer page, Integer size) {
        return of(page, size, DEFAULT_SIZE);
    }

    /**
     * Сохраняет явно заданный endpoint размер страницы по умолчанию.
     */
    public static PageParameters of(Integer page, Integer size, int defaultSize) {
        return new PageParameters(page == null ? 0 : page, size == null ? defaultSize : size);
    }

    /**
     * Создаёт запрос страницы для репозитория.
     */
    public PageRequest toPageable() {
        return PageRequest.of(page, size);
    }
}
