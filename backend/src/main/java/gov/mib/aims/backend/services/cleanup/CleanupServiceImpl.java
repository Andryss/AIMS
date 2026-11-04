package gov.mib.aims.backend.services.cleanup;

import gov.mib.aims.backend.entity.CleanupReportEntity;
import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.ChangeCleanupStatusRequest;
import gov.mib.aims.backend.generated.model.CleanupReportResponse;
import gov.mib.aims.backend.generated.model.CreateCleanupReportRequest;
import gov.mib.aims.backend.generated.model.IncidentResponse;
import gov.mib.aims.backend.model.CleanupStatus;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.repository.CleanupReportRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.auth.CurrentUserService;
import gov.mib.aims.backend.services.cleanup.status.CleanupStatusWorkflow;
import gov.mib.aims.backend.services.incident.IncidentHistoryService;
import gov.mib.aims.backend.services.mapping.CleanupMapper;
import gov.mib.aims.backend.services.mapping.IncidentMapper;
import gov.mib.aims.backend.services.transaction.RetryableSerializable;
import gov.mib.aims.backend.services.validation.AttachmentValidator;
import gov.mib.aims.backend.services.validation.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Реализация {@link CleanupService}.
 */
@Service
@RequiredArgsConstructor
public class CleanupServiceImpl implements CleanupService {

    private final IncidentRepository incidentRepository;
    private final CleanupReportRepository cleanupReportRepository;
    private final CurrentUserService currentUserService;
    private final IncidentHistoryService incidentHistoryService;
    private final CleanupStatusWorkflow cleanupStatusWorkflow;
    private final CleanupMapper cleanupMapper;
    private final IncidentMapper incidentMapper;
    private final AttachmentValidator attachmentValidator;
    private final ChronoService chronoService;

    @Override
    @RetryableSerializable
    public CleanupReportResponse createReport(Long incidentId, CreateCleanupReportRequest request) {
        IncidentEntity incident = incidentRepository.findById(incidentId)
                .orElseThrow(Errors::incidentNotFound);
        assertCleanupAllowed(incident);
        if (cleanupReportRepository.existsByIncidentId(incidentId)) {
            throw Errors.cleanupReportAlreadyExists();
        }
        attachmentValidator.assertAllExist(request.getAttachmentFileIds());

        LocalDateTime now = chronoService.localDateTimeNow();
        CleanupReportEntity report = CleanupReportEntity.builder()
                .incidentId(incidentId)
                .description(TextNormalizer.required(request.getDescription(), "Укажите описание"))
                .attachmentFileIds(new ArrayList<>(request.getAttachmentFileIds()))
                .createdByUserId(currentUserService.getCurrentUserId())
                .createdAt(now)
                .build();
        report = cleanupReportRepository.save(report);

        incident.setCleanupReportId(report.getId());
        incident.setUpdatedAt(now);
        incident = incidentRepository.save(incident);
        incidentHistoryService.record(incident);
        return cleanupMapper.toReportResponse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public CleanupReportResponse getReport(Long incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw Errors.incidentNotFound();
        }
        CleanupReportEntity report = cleanupReportRepository.findByIncidentId(incidentId)
                .orElseThrow(Errors::cleanupReportNotFound);
        return cleanupMapper.toReportResponse(report);
    }

    @Override
    @RetryableSerializable
    public IncidentResponse changeCleanupStatus(Long incidentId, ChangeCleanupStatusRequest request) {
        IncidentEntity incident = incidentRepository.findById(incidentId)
                .orElseThrow(Errors::incidentNotFound);
        CleanupStatus target = cleanupMapper.toModelCleanupStatus(request.getStatus());
        CleanupStatus previous = incident.getCleanupStatus();
        incident = cleanupStatusWorkflow.changeStatus(incident, target);
        incident = incidentRepository.save(incident);
        incidentHistoryService.record(incident);
        cleanupStatusWorkflow.afterSave(incident, previous);
        return incidentMapper.toResponse(incident);
    }

    private void assertCleanupAllowed(IncidentEntity incident) {
        IncidentStatus status = incident.getStatus();
        if (status != IncidentStatus.EXECUTING && status != IncidentStatus.EXECUTION_COMPLETED) {
            throw Errors.cleanupNotAllowed();
        }
    }
}
