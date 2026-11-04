package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.IncidentCommentEntity;
import gov.mib.aims.backend.generated.model.IncidentCommentListResponse;
import gov.mib.aims.backend.generated.model.IncidentCommentResponse;
import gov.mib.aims.backend.services.ChronoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Преобразование данных в представления API.
 */
@Component
@RequiredArgsConstructor
public class IncidentCommentMapper {

    private final ChronoService chronoService;

    /**
     * Создаёт представление, независимое от исходной сущности.
     */
    public IncidentCommentResponse toResponse(IncidentCommentEntity entity) {
        return new IncidentCommentResponse()
                .id(entity.getId())
                .incidentId(entity.getIncidentId())
                .authorUserId(entity.getAuthorUserId())
                .text(entity.getText())
                .createdAt(chronoService.toOffsetDateTime(entity.getCreatedAt()));
    }

    /**
     * Собирает элементы и метаданные страницы для API.
     */
    public IncidentCommentListResponse toListResponse(Page<IncidentCommentEntity> page) {
        return new IncidentCommentListResponse().items(page.map(this::toResponse).getContent())
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages());
    }
}
