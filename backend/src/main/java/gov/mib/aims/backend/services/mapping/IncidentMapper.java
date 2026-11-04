package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.generated.model.CleanupStatusApi;
import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.generated.model.IncidentListResponse;
import gov.mib.aims.backend.generated.model.IncidentResponse;
import gov.mib.aims.backend.generated.model.IncidentStatusApi;
import gov.mib.aims.backend.model.CleanupStatus;
import gov.mib.aims.backend.model.IncidentEventType;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.services.ChronoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Маппинг инцидента между доменной моделью и OpenAPI DTO.
 */
@Component
@RequiredArgsConstructor
public class IncidentMapper {

    private final ChronoService chronoService;

    /**
     * Преобразует сущность инцидента в ответ API.
     *
     * @param entity сущность
     * @return DTO ответа
     */
    public IncidentResponse toResponse(IncidentEntity entity) {
        List<Long> executorIds = entity.getExecutorUserIds() != null
                ? new ArrayList<>(entity.getExecutorUserIds())
                : new ArrayList<>();
        return new IncidentResponse()
                .id(entity.getId())
                .status(toApiStatus(entity.getStatus()))
                .eventType(toApiEventType(entity.getEventType()))
                .location(entity.getLocation())
                .detectedAt(chronoService.toOffsetDateTime(entity.getDetectedAt()))
                .description(entity.getDescription())
                .victimCount(entity.getVictimCount())
                .threatLevel(entity.getThreatLevel())
                .attachmentFileIds(new ArrayList<>(entity.getAttachmentFileIds()))
                .createdAt(chronoService.toOffsetDateTime(entity.getCreatedAt()))
                .updatedAt(chronoService.toOffsetDateTime(entity.getUpdatedAt()))
                .alienId(entity.getAlienId())
                .responsibleUserId(entity.getResponsibleUserId())
                .executorUserIds(executorIds)
                .cleanupStatus(toApiCleanupStatus(entity.getCleanupStatus()))
                .cleanupReportId(entity.getCleanupReportId())
                .monitoringAlertId(entity.getMonitoringAlertId());
    }

    /**
     * Преобразует тип события из API в доменную модель.
     */
    public IncidentEventType toModelEventType(IncidentEventTypeApi dto) {
        return switch (dto) {
            case UNIDENTIFIED_SIGHTING -> IncidentEventType.UNIDENTIFIED_SIGHTING;
            case CONTACT_SUSPECT -> IncidentEventType.CONTACT_SUSPECT;
            case ILLEGAL_UFO_LANDING -> IncidentEventType.ILLEGAL_UFO_LANDING;
            case MEMORY_ANOMALY -> IncidentEventType.MEMORY_ANOMALY;
            case ALIEN_ARTIFACT -> IncidentEventType.ALIEN_ARTIFACT;
            case ALIEN_CAPTURE -> IncidentEventType.ALIEN_CAPTURE;
            case ATTACK_ON_PEOPLE -> IncidentEventType.ATTACK_ON_PEOPLE;
            case PROPERTY_DAMAGE -> IncidentEventType.PROPERTY_DAMAGE;
            case BIOLOGICAL_CONTAMINATION -> IncidentEventType.BIOLOGICAL_CONTAMINATION;
            case CRITICAL_INFRASTRUCTURE_DISRUPTION -> IncidentEventType.CRITICAL_INFRASTRUCTURE_DISRUPTION;
        };
    }

    /**
     * Преобразует статус инцидента из API в доменную модель.
     */
    public IncidentStatus toModelStatus(IncidentStatusApi dto) {
        return switch (dto) {
            case DRAFT -> IncidentStatus.DRAFT;
            case READY_FOR_ANALYSIS -> IncidentStatus.READY_FOR_ANALYSIS;
            case READY_FOR_EXECUTION -> IncidentStatus.READY_FOR_EXECUTION;
            case CLARIFICATION_REQUIRED -> IncidentStatus.CLARIFICATION_REQUIRED;
            case PREPARATION_FOR_EXECUTION -> IncidentStatus.PREPARATION_FOR_EXECUTION;
            case PREPARED_FOR_EXECUTION -> IncidentStatus.PREPARED_FOR_EXECUTION;
            case EXECUTING -> IncidentStatus.EXECUTING;
            case EXECUTION_COMPLETED -> IncidentStatus.EXECUTION_COMPLETED;
            case REANALYSIS_REQUIRED -> IncidentStatus.REANALYSIS_REQUIRED;
        };
    }

    /**
     * Преобразует статус инцидента в API enum.
     */
    public IncidentStatusApi toApiStatus(IncidentStatus status) {
        return switch (status) {
            case DRAFT -> IncidentStatusApi.DRAFT;
            case READY_FOR_ANALYSIS -> IncidentStatusApi.READY_FOR_ANALYSIS;
            case READY_FOR_EXECUTION -> IncidentStatusApi.READY_FOR_EXECUTION;
            case CLARIFICATION_REQUIRED -> IncidentStatusApi.CLARIFICATION_REQUIRED;
            case PREPARATION_FOR_EXECUTION -> IncidentStatusApi.PREPARATION_FOR_EXECUTION;
            case PREPARED_FOR_EXECUTION -> IncidentStatusApi.PREPARED_FOR_EXECUTION;
            case EXECUTING -> IncidentStatusApi.EXECUTING;
            case EXECUTION_COMPLETED -> IncidentStatusApi.EXECUTION_COMPLETED;
            case REANALYSIS_REQUIRED -> IncidentStatusApi.REANALYSIS_REQUIRED;
        };
    }

    /**
     * Преобразует тип события в API enum.
     */
    public IncidentEventTypeApi toApiEventType(IncidentEventType eventType) {
        return switch (eventType) {
            case UNIDENTIFIED_SIGHTING -> IncidentEventTypeApi.UNIDENTIFIED_SIGHTING;
            case CONTACT_SUSPECT -> IncidentEventTypeApi.CONTACT_SUSPECT;
            case ILLEGAL_UFO_LANDING -> IncidentEventTypeApi.ILLEGAL_UFO_LANDING;
            case MEMORY_ANOMALY -> IncidentEventTypeApi.MEMORY_ANOMALY;
            case ALIEN_ARTIFACT -> IncidentEventTypeApi.ALIEN_ARTIFACT;
            case ALIEN_CAPTURE -> IncidentEventTypeApi.ALIEN_CAPTURE;
            case ATTACK_ON_PEOPLE -> IncidentEventTypeApi.ATTACK_ON_PEOPLE;
            case PROPERTY_DAMAGE -> IncidentEventTypeApi.PROPERTY_DAMAGE;
            case BIOLOGICAL_CONTAMINATION -> IncidentEventTypeApi.BIOLOGICAL_CONTAMINATION;
            case CRITICAL_INFRASTRUCTURE_DISRUPTION -> IncidentEventTypeApi.CRITICAL_INFRASTRUCTURE_DISRUPTION;
        };
    }

    /**
     * Преобразует статус очистки в API enum.
     */
    public CleanupStatusApi toApiCleanupStatus(CleanupStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PREPARATION -> CleanupStatusApi.PREPARATION;
            case EXECUTION -> CleanupStatusApi.EXECUTION;
            case COMPLETED -> CleanupStatusApi.COMPLETED;
        };
    }

    /**
     * Собирает элементы и метаданные страницы для API.
     */
    public IncidentListResponse toListResponse(Page<IncidentEntity> page) {
        return new IncidentListResponse().items(page.map(this::toResponse).getContent())
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages());
    }
}
