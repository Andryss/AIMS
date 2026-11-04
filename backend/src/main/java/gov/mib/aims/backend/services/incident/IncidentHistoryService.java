package gov.mib.aims.backend.services.incident;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.services.history.EntityHistoryService;
import gov.mib.aims.backend.services.mapping.IncidentSnapshotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Записывает независимый снимок инцидента в транзакции изменяющей его команды.
 */
@Component
@RequiredArgsConstructor
public class IncidentHistoryService {

    private final EntityHistoryService historyService;
    private final IncidentSnapshotMapper mapper;

    /**
     * Сохраняет состояние, не привязывая JSON к JPA-классу.
     */
    public void record(IncidentEntity incident) {
        historyService.recordChange(EntityType.INCIDENT, incident.getId(), mapper.toStorage(incident));
    }
}
