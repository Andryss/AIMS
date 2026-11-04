package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.IncidentResponse;
import gov.mib.aims.backend.generated.model.IncidentThreatCalculationResponse;
import gov.mib.aims.backend.generated.model.IncidentThreatContribution;
import gov.mib.aims.backend.generated.model.SetIncidentThreatLevelRequest;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.model.IncidentThreatAssessment;
import gov.mib.aims.backend.repository.AlienRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.services.incident.IncidentThreatCalculator;
import gov.mib.aims.backend.services.mapping.IncidentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Предлагает и сохраняет явно принятую оценку угрозы инцидента.
 */
@Service
@RequiredArgsConstructor
public class IncidentThreatService {

    private final IncidentRepository incidentRepository;
    private final AlienRepository alienRepository;
    private final IncidentThreatCalculator calculator;
    private final IncidentMapper incidentMapper;
    private final EntityHistoryService entityHistoryService;
    private final ChronoService chronoService;

    /**
     * Рассчитывает предложение по согласованному снимку данных без сохранения.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public IncidentThreatCalculationResponse calculate(Long id) {
        var incident = findOpenIncident(id);
        var result = calculate(incident);
        var contributions = result.contributions().stream().map(contribution -> new IncidentThreatContribution()
                .criterion(IncidentThreatContribution.CriterionEnum.valueOf(contribution.criterion().name()))
                .adjustment(contribution.adjustment())).toList();

        return new IncidentThreatCalculationResponse()
                .calculatedThreatLevel(result.calculatedThreatLevel())
                .contributions(contributions);
    }

    /**
     * Сохраняет выбранный уровень угрозы с историей изменений.
     */
    @Retryable(interceptor = "retryInterceptor")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public IncidentResponse setThreatLevel(Long id, SetIncidentThreatLevelRequest request) {
        var incident = findOpenIncident(id);
        int level = request.getThreatLevel();
        if (Objects.equals(incident.getThreatLevel(), level)) {
            return incidentMapper.toResponse(incident);
        }

        incident.setThreatLevel(level);
        incident.setUpdatedAt(chronoService.localDateTimeNow());
        incident = incidentRepository.save(incident);
        entityHistoryService.recordChange(EntityType.INCIDENT, incident.getId(), incident);

        return incidentMapper.toResponse(incident);
    }

    private IncidentEntity findOpenIncident(Long id) {
        var incident = incidentRepository.findById(id).orElseThrow(Errors::incidentNotFound);
        if (incident.getStatus() == IncidentStatus.EXECUTION_COMPLETED) {
            throw Errors.incidentConflict("Для завершённого инцидента нельзя изменить оценку угрозы");
        }

        return incident;
    }

    private IncidentThreatAssessment calculate(IncidentEntity incident) {
        Integer alienThreatLevel = null;
        if (incident.getAlienId() != null) {
            var alien = alienRepository.findById(incident.getAlienId()).orElseThrow(Errors::alienNotFound);
            alienThreatLevel = alien.getThreatLevel().intValue();
        }

        return calculator.calculate(incident.getEventType(), incident.getVictimCount(), alienThreatLevel);
    }
}
