package gov.mib.aims.backend.services;

import gov.mib.aims.backend.generated.model.ChangeCleanupStatusRequest;
import gov.mib.aims.backend.generated.model.CleanupReportResponse;
import gov.mib.aims.backend.generated.model.CreateCleanupReportRequest;
import gov.mib.aims.backend.generated.model.IncidentResponse;

/**
 * Сервис отчётов об очистке и статуса очистки.
 */
public interface CleanupService {

    /**
     * Создаёт отчёт об очистке (один на инцидент).
     *
     * @param incidentId идентификатор инцидента
     * @param request описание и вложения
     * @return созданный отчёт
     */
    CleanupReportResponse createReport(Long incidentId, CreateCleanupReportRequest request);

    /**
     * Возвращает отчёт об очистке по инциденту.
     *
     * @param incidentId идентификатор инцидента
     * @return отчёт
     */
    CleanupReportResponse getReport(Long incidentId);

    /**
     * Меняет статус очистки инцидента.
     *
     * @param incidentId идентификатор инцидента
     * @param request целевой статус
     * @return обновлённый инцидент
     */
    IncidentResponse changeCleanupStatus(Long incidentId, ChangeCleanupStatusRequest request);
}
