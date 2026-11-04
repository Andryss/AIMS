package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.TenderParticipantEntity;
import gov.mib.aims.backend.generated.model.TenderParticipantListResponse;
import gov.mib.aims.backend.generated.model.TenderParticipantResponse;
import gov.mib.aims.backend.generated.model.TenderParticipantStatusApi;
import gov.mib.aims.backend.services.ChronoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Представление предложения в API и снимках истории.
 */
@Component
@RequiredArgsConstructor
public class TenderParticipantMapper {

    private final ChronoService chronoService;

    /**
     * Создаёт независимый снимок предложения участника.
     */
    public TenderParticipantResponse toResponse(TenderParticipantEntity entity) {
        return new TenderParticipantResponse().id(entity.getId()).tenderId(entity.getTenderId())
                .name(entity.getName()).proposal(entity.getProposal())
                .status(TenderParticipantStatusApi.valueOf(entity.getStatus().name()))
                .createdByUserId(entity.getCreatedByUserId())
                .createdAt(chronoService.toOffsetDateTime(entity.getCreatedAt()))
                .updatedAt(chronoService.toOffsetDateTime(entity.getUpdatedAt()));
    }

    /**
     * Собирает элементы и метаданные страницы для API.
     */
    public TenderParticipantListResponse toListResponse(Page<TenderParticipantEntity> page) {
        return new TenderParticipantListResponse().items(page.map(this::toResponse).getContent())
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages());
    }
}
