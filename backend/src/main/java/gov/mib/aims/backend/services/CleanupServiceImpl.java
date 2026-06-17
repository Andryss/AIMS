package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.CleanupReportEntity;
import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.ChangeCleanupStatusRequest;
import gov.mib.aims.backend.generated.model.CleanupReportResponse;
import gov.mib.aims.backend.generated.model.CreateCleanupReportRequest;
import gov.mib.aims.backend.generated.model.IncidentResponse;
import gov.mib.aims.backend.model.CleanupStatus;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.repository.CleanupReportRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.services.cleanup.status.CleanupStatusWorkflow;
import gov.mib.aims.backend.services.mapping.CleanupMapper;
import gov.mib.aims.backend.services.mapping.IncidentMapper;
import gov.mib.aims.backend.services.validation.AttachmentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
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
    private final EntityHistoryService entityHistoryService;
    private final CleanupStatusWorkflow cleanupStatusWorkflow;
    private final CleanupMapper cleanupMapper;
    private final IncidentMapper incidentMapper;
    private final AttachmentValidator attachmentValidator;
    private final Clock clock;

    @Override
    @Transactional
    public CleanupReportResponse createReport(Long incidentId, CreateCleanupReportRequest request) {
        IncidentEntity incident = incidentRepository.findById(incidentId)
                .orElseThrow(Errors::incidentNotFound);
        assertCleanupAllowed(incident);
        if (cleanupReportRepository.existsByIncidentId(incidentId)) {
            throw Errors.cleanupReportAlreadyExists();
        }
        attachmentValidator.assertAllExist(request.getAttachmentFileIds());

        LocalDateTime now = LocalDateTime.now(clock);
        CleanupReportEntity report = CleanupReportEntity.builder()
                .incidentId(incidentId)
                .description(request.getDescription().trim())
                .attachmentFileIds(new ArrayList<>(request.getAttachmentFileIds()))
                .createdByUserId(currentUserService.getCurrentUserId())
                .createdAt(now)
                .build();
        report = cleanupReportRepository.save(report);

        incident.setCleanupReportId(report.getId());
        incident.setUpdatedAt(now);
        incident = incidentRepository.save(incident);
        entityHistoryService.recordChange(EntityType.INCIDENT, incident.getId(), incident);
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
    @Transactional
    public IncidentResponse changeCleanupStatus(Long incidentId, ChangeCleanupStatusRequest request) {
        IncidentEntity incident = incidentRepository.findById(incidentId)
                .orElseThrow(Errors::incidentNotFound);
        CleanupStatus target = cleanupMapper.toModelCleanupStatus(request.getStatus());
        incident = cleanupStatusWorkflow.changeStatus(incident, target);
        entityHistoryService.recordChange(EntityType.INCIDENT, incident.getId(), incident);
        return incidentMapper.toResponse(incident);
    }

    private void assertCleanupAllowed(IncidentEntity incident) {
        IncidentStatus status = incident.getStatus();
        if (status != IncidentStatus.EXECUTING && status != IncidentStatus.EXECUTION_COMPLETED) {
            throw Errors.cleanupNotAllowed();
        }
    }
}
