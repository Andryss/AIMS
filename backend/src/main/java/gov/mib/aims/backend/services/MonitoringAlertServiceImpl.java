package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.MonitoringAlertEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.IngestMonitoringEventRequest;
import gov.mib.aims.backend.generated.model.MonitoringAlertListResponse;
import gov.mib.aims.backend.generated.model.MonitoringAlertResponse;
import gov.mib.aims.backend.generated.model.MonitoringAlertStatusApi;
import gov.mib.aims.backend.model.MonitoringAlertStatus;
import gov.mib.aims.backend.repository.MonitoringAlertRepository;
import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyOperatorsMonitoringAlertPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyOperatorsMonitoringAlertProcessor;
import gov.mib.aims.backend.services.mapping.IncidentMapper;
import gov.mib.aims.backend.services.mapping.MonitoringAlertMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Реализация {@link MonitoringAlertService}.
 */
@Service
@RequiredArgsConstructor
public class MonitoringAlertServiceImpl implements MonitoringAlertService {

    private static final String DEFAULT_SOURCE_SYSTEM = "EXTERNAL_MONITORING_V1";

    private final MonitoringAlertRepository monitoringAlertRepository;
    private final MonitoringAlertMapper monitoringAlertMapper;
    private final IncidentMapper incidentMapper;
    private final ObjectMapperWrapper objectMapper;
    private final DbQueueService dbQueueService;
    private final Clock clock;

    @Override
    @Transactional
    public MonitoringAlertResponse ingest(IngestMonitoringEventRequest request) {
        String externalEventId = request.getExternalEventId().trim();
        if (monitoringAlertRepository.existsByExternalEventId(externalEventId)) {
            throw Errors.duplicateMonitoringEvent();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<String> mediaUrls = request.getMediaUrls() != null
                ? request.getMediaUrls().stream().map(java.net.URI::toString).toList()
                : new ArrayList<>();
        Map<String, Object> rawPayload = objectMapper.readMap(
                objectMapper.writeValueAsStringOrThrow(request)
        );

        MonitoringAlertEntity entity = MonitoringAlertEntity.builder()
                .externalEventId(externalEventId)
                .sourceSystem(resolveSourceSystem(request.getSourceSystem()))
                .status(MonitoringAlertStatus.NEW)
                .eventType(incidentMapper.toModelEventType(request.getEventType()))
                .location(request.getLocation().trim())
                .detectedAt(request.getDetectedAt().toLocalDateTime())
                .description(request.getDescription().trim())
                .mediaUrls(mediaUrls)
                .rawPayload(rawPayload)
                .receivedAt(now)
                .createdAt(now)
                .build();
        entity = monitoringAlertRepository.save(entity);

        dbQueueService.produceTask(
                NotifyOperatorsMonitoringAlertProcessor.class,
                new NotifyOperatorsMonitoringAlertPayload(entity.getId())
        );

        return monitoringAlertMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public MonitoringAlertListResponse list(MonitoringAlertStatusApi status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<MonitoringAlertEntity> result;
        MonitoringAlertStatus modelStatus = monitoringAlertMapper.toModelStatus(status);
        if (modelStatus != null) {
            result = monitoringAlertRepository.findByStatusOrderByReceivedAtDesc(modelStatus, pageable);
        } else {
            result = monitoringAlertRepository.findAllByOrderByReceivedAtDesc(pageable);
        }
        return new MonitoringAlertListResponse()
                .items(result.getContent().stream().map(monitoringAlertMapper::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public MonitoringAlertResponse getById(Long id) {
        MonitoringAlertEntity entity = monitoringAlertRepository.findById(id)
                .orElseThrow(Errors::monitoringAlertNotFound);
        return monitoringAlertMapper.toResponse(entity);
    }

    private String resolveSourceSystem(String sourceSystem) {
        if (sourceSystem == null || sourceSystem.isBlank()) {
            return DEFAULT_SOURCE_SYSTEM;
        }
        return sourceSystem.trim();
    }
}
