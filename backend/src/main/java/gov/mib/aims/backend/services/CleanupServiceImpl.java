package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.CleanupReportEntity;
import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.ChangeCleanupStatusRequest;
import gov.mib.aims.backend.generated.model.CleanupReportResponse;
import gov.mib.aims.backend.generated.model.CleanupStatusApi;
import gov.mib.aims.backend.generated.model.CreateCleanupReportRequest;
import gov.mib.aims.backend.generated.model.IncidentResponse;
import gov.mib.aims.backend.model.CleanupStatus;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.repository.CleanupReportRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.repository.StoredFileRepository;
import gov.mib.aims.backend.services.cleanup.status.CleanupStatusWorkflow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Реализация {@link CleanupService}.
 */
@Service
@RequiredArgsConstructor
public class CleanupServiceImpl implements CleanupService {

    private final IncidentRepository incidentRepository;
    private final CleanupReportRepository cleanupReportRepository;
    private final StoredFileRepository storedFileRepository;
    private final CurrentUserService currentUserService;
    private final EntityHistoryService entityHistoryService;
    private final CleanupStatusWorkflow cleanupStatusWorkflow;
    private final IncidentService incidentService;
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
        String description = request.getDescription() != null ? request.getDescription().trim() : "";
        if (description.isEmpty()) {
            throw Errors.validationError("Description must not be empty");
        }
        List<Long> attachmentFileIds = request.getAttachmentFileIds();
        if (attachmentFileIds == null || attachmentFileIds.isEmpty()) {
            throw Errors.validationError("At least one attachment is required");
        }
        assertAttachmentsExist(attachmentFileIds);

        LocalDateTime now = LocalDateTime.now(clock);
        CleanupReportEntity report = CleanupReportEntity.builder()
                .incidentId(incidentId)
                .description(description)
                .attachmentFileIds(new ArrayList<>(attachmentFileIds))
                .createdByUserId(currentUserService.getCurrentUserId())
                .createdAt(now)
                .build();
        report = cleanupReportRepository.save(report);

        incident.setCleanupReportId(report.getId());
        incident.setUpdatedAt(now);
        incident = incidentRepository.save(incident);
        entityHistoryService.recordChange(EntityType.INCIDENT, incident.getId(), incident);
        return toReportResponse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public CleanupReportResponse getReport(Long incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw Errors.incidentNotFound();
        }
        CleanupReportEntity report = cleanupReportRepository.findByIncidentId(incidentId)
                .orElseThrow(Errors::cleanupReportNotFound);
        return toReportResponse(report);
    }

    @Override
    @Transactional
    public IncidentResponse changeCleanupStatus(Long incidentId, ChangeCleanupStatusRequest request) {
        IncidentEntity incident = incidentRepository.findById(incidentId)
                .orElseThrow(Errors::incidentNotFound);
        CleanupStatus target = toModelCleanupStatus(request.getStatus());
        incident = cleanupStatusWorkflow.changeStatus(incident, target);
        entityHistoryService.recordChange(EntityType.INCIDENT, incident.getId(), incident);
        return incidentService.getById(incidentId);
    }

    private void assertCleanupAllowed(IncidentEntity incident) {
        IncidentStatus status = incident.getStatus();
        if (status != IncidentStatus.EXECUTING && status != IncidentStatus.EXECUTION_COMPLETED) {
            throw Errors.cleanupNotAllowed();
        }
    }

    private void assertAttachmentsExist(List<Long> attachmentFileIds) {
        for (Long fileId : attachmentFileIds) {
            if (!storedFileRepository.existsById(fileId)) {
                throw Errors.attachmentNotFound();
            }
        }
    }

    private CleanupStatus toModelCleanupStatus(CleanupStatusApi dto) {
        return switch (dto) {
            case PREPARATION -> CleanupStatus.PREPARATION;
            case EXECUTION -> CleanupStatus.EXECUTION;
            case COMPLETED -> CleanupStatus.COMPLETED;
        };
    }

    private CleanupReportResponse toReportResponse(CleanupReportEntity entity) {
        return new CleanupReportResponse()
                .id(entity.getId())
                .incidentId(entity.getIncidentId())
                .description(entity.getDescription())
                .attachmentFileIds(new ArrayList<>(entity.getAttachmentFileIds()))
                .createdByUserId(entity.getCreatedByUserId())
                .createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
}
