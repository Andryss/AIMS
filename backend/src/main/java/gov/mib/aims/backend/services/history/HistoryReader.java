package gov.mib.aims.backend.services.history;

import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.HistoryRecord;
import gov.mib.aims.backend.repository.EntityHistoryRepository;
import gov.mib.aims.backend.services.ObjectMapperWrapper;
import gov.mib.aims.backend.services.query.PageParameters;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Читает снимки любого домена в устойчивом хронологическом порядке.
 */
@Component
@RequiredArgsConstructor
public class HistoryReader {

    private final EntityHistoryRepository repository;
    private final ObjectMapperWrapper objectMapper;

    /**
     * Возвращает данные аудита и декодированный снимок в формате его хранения.
     */
    public <S> Page<HistoryRecord<S>> read(EntityType type, Long id, int page, int size, Class<S> snapshotType) {
        return repository.findByEntityTypeAndEntityIdOrderByChangedAtAscIdAsc(type, id,
                PageParameters.of(page, size).toPageable()).map(entry -> new HistoryRecord<>(
                        entry.getId(), entry.getChangedAt(), entry.getChangedByUserId(),
                        objectMapper.readValue(entry.getSnapshot(), snapshotType)));
    }
}
