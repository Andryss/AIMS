package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.MonitoringAlertEntity;
import gov.mib.aims.backend.generated.model.MonitoringAlertResponse;
import gov.mib.aims.backend.generated.model.MonitoringAlertStatusApi;
import gov.mib.aims.backend.model.MonitoringAlertStatus;
import gov.mib.aims.backend.services.ChronoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Маппинг алерта мониторинга между доменной моделью и OpenAPI DTO.
 */
@Component
@RequiredArgsConstructor
public class MonitoringAlertMapper {

    private final ChronoService chronoService;

    private final IncidentMapper incidentMapper;

    /**
     * Преобразует сущность алерта в ответ API.
     */
    public MonitoringAlertResponse toResponse(MonitoringAlertEntity entity) {
        return new MonitoringAlertResponse()
                .id(entity.getId())
                .externalEventId(entity.getExternalEventId())
                .sourceSystem(entity.getSourceSystem())
                .status(toApiStatus(entity.getStatus()))
                .eventType(incidentMapper.toApiEventType(entity.getEventType()))
                .location(entity.getLocation())
                .detectedAt(chronoService.toOffsetDateTime(entity.getDetectedAt()))
                .description(entity.getDescription())
                .mediaUrls(entity.getMediaUrls() != null
                        ? new ArrayList<>(entity.getMediaUrls())
                        : new ArrayList<>())
                .incidentId(entity.getIncidentId())
                .receivedAt(chronoService.toOffsetDateTime(entity.getReceivedAt()))
                .createdAt(chronoService.toOffsetDateTime(entity.getCreatedAt()));
    }

    /**
     * Преобразует статус алерта из API в доменную модель.
     */
    public MonitoringAlertStatus toModelStatus(MonitoringAlertStatusApi status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case NEW -> MonitoringAlertStatus.NEW;
            case INCIDENT_CREATED -> MonitoringAlertStatus.INCIDENT_CREATED;
        };
    }

    /**
     * Преобразует статус алерта в API enum.
     */
    public MonitoringAlertStatusApi toApiStatus(MonitoringAlertStatus status) {
        return switch (status) {
            case NEW -> MonitoringAlertStatusApi.NEW;
            case INCIDENT_CREATED -> MonitoringAlertStatusApi.INCIDENT_CREATED;
        };
    }
}
