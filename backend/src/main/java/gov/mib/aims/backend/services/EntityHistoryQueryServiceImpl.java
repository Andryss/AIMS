package gov.mib.aims.backend.services;

import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.IncidentHistoryListResponse;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.IncidentSnapshot;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.services.history.HistoryReader;
import gov.mib.aims.backend.services.mapping.IncidentSnapshotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Читает историю инцидента с независимой моделью сохраняемого снимка.
 */
@Service
@RequiredArgsConstructor
public class EntityHistoryQueryServiceImpl implements EntityHistoryQueryService {

    private final IncidentRepository incidentRepository;
    private final HistoryReader historyReader;
    private final IncidentSnapshotMapper snapshotMapper;

    @Override
    @Transactional(readOnly = true)
    public IncidentHistoryListResponse listIncidentHistory(Long incidentId, int page, int size) {
        if (!incidentRepository.existsById(incidentId)) {
            throw Errors.incidentNotFound();
        }
        var result = historyReader.read(EntityType.INCIDENT, incidentId, page, size, IncidentSnapshot.class);

        return snapshotMapper.toHistoryListResponse(result);
    }
}
