package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.CleanupReportEntity;
import gov.mib.aims.backend.generated.model.CleanupReportResponse;
import gov.mib.aims.backend.generated.model.CleanupStatusApi;
import gov.mib.aims.backend.model.CleanupStatus;
import gov.mib.aims.backend.services.ChronoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Маппинг отчёта об очистке и статуса очистки между доменной моделью и OpenAPI DTO.
 */
@Component
@RequiredArgsConstructor
public class CleanupMapper {

    private final ChronoService chronoService;

    /**
     * Преобразует статус очистки из API в доменную модель.
     */
    public CleanupStatus toModelCleanupStatus(CleanupStatusApi dto) {
        return switch (dto) {
            case PREPARATION -> CleanupStatus.PREPARATION;
            case EXECUTION -> CleanupStatus.EXECUTION;
            case COMPLETED -> CleanupStatus.COMPLETED;
        };
    }

    /**
     * Преобразует сущность отчёта в ответ API.
     */
    public CleanupReportResponse toReportResponse(CleanupReportEntity entity) {
        return new CleanupReportResponse()
                .id(entity.getId())
                .incidentId(entity.getIncidentId())
                .description(entity.getDescription())
                .attachmentFileIds(new ArrayList<>(entity.getAttachmentFileIds()))
                .createdByUserId(entity.getCreatedByUserId())
                .createdAt(chronoService.toOffsetDateTime(entity.getCreatedAt()));
    }
}
