package gov.mib.aims.backend.services;

import gov.mib.aims.backend.generated.model.SaveTenderParticipantRequest;
import gov.mib.aims.backend.generated.model.TenderParticipantListResponse;
import gov.mib.aims.backend.generated.model.TenderParticipantResponse;

/**
 * Предложения участников тендера.
 */
public interface TenderParticipantService {

    /**
     * Возвращает страницу предложений указанного тендера.
     */
    TenderParticipantListResponse list(Long tenderId, int page, int size);

    /**
     * Добавляет предложение во время сбора.
     */
    TenderParticipantResponse create(Long tenderId, SaveTenderParticipantRequest request);

    /**
     * Редактирует предложение, принадлежащее указанному тендеру.
     */
    TenderParticipantResponse update(Long tenderId, Long participantId, SaveTenderParticipantRequest request);
}
