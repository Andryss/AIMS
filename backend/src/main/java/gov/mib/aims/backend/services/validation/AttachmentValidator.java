package gov.mib.aims.backend.services.validation;

import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Проверяет ссылки на файлы и объединяет вложения с доменным ограничением размера.
 */
@Component
@RequiredArgsConstructor
public class AttachmentValidator {

    private final StoredFileRepository storedFileRepository;

    /** Проверяет существование всех уникальных id одним запросом. */
    public void assertAllExist(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw Errors.validationError("Укажите положительный идентификатор каждого файла");
        }
        var distinct = new LinkedHashSet<>(ids);
        if (storedFileRepository.countByIdIn(distinct) != distinct.size()) {
            throw Errors.attachmentNotFound();
        }
    }

    /** Проверяет самостоятельный список фотографий с заданным доменным лимитом. */
    public void assertDistinctWithinLimit(List<Long> ids, int max, String message) {
        if (ids == null || ids.size() > max || ids.stream().anyMatch(id -> id == null || id <= 0)
                || new LinkedHashSet<>(ids).size() != ids.size()) {
            throw Errors.validationError(message);
        }
        assertAllExist(ids);
    }

    /** Добавляет новые вложения без повторов, сохраняя порядок существующих файлов. */
    public List<Long> merge(List<Long> existing, List<Long> additional, int max) {
        List<Long> incoming = additional == null ? List.of() : additional;
        if (incoming.stream().anyMatch(Objects::isNull)) {
            throw Errors.validationError("Укажите идентификатор каждого добавляемого файла");
        }
        assertAllExist(incoming);
        var merged = new LinkedHashSet<>(existing == null ? List.<Long>of() : existing);
        merged.addAll(incoming);
        if (merged.size() > max) {
            throw Errors.validationError("Можно прикрепить не более " + max + " файлов");
        }

        return new ArrayList<>(merged);
    }
}
