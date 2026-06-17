package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.CleanupReportEntity;
import gov.mib.aims.backend.generated.model.CleanupReportResponse;
import gov.mib.aims.backend.generated.model.CleanupStatusApi;
import gov.mib.aims.backend.model.CleanupStatus;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.ArrayList;

/**
 * Маппинг отчёта об очистке и статуса очистки между доменной моделью и OpenAPI DTO.
 */
@Component
public class CleanupMapper {

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
                .createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
}
