package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.EquipmentEntity;
import gov.mib.aims.backend.entity.EquipmentMovementEntity;
import gov.mib.aims.backend.entity.EquipmentMovementItemEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.EquipmentMovementHistoryEntry;
import gov.mib.aims.backend.generated.model.ChangeEquipmentMovementStatusRequest;
import gov.mib.aims.backend.generated.model.EquipmentMovementHistoryListResponse;
import gov.mib.aims.backend.generated.model.EquipmentMovementHistorySnapshot;
import gov.mib.aims.backend.generated.model.EquipmentMovementListResponse;
import gov.mib.aims.backend.generated.model.EquipmentMovementOperationApi;
import gov.mib.aims.backend.generated.model.EquipmentMovementResponse;
import gov.mib.aims.backend.generated.model.EquipmentMovementStatusApi;
import gov.mib.aims.backend.generated.model.EquipmentResponse;
import gov.mib.aims.backend.generated.model.SaveEquipmentMovementRequest;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.EquipmentMovementOperation;
import gov.mib.aims.backend.model.EquipmentMovementStatus;
import gov.mib.aims.backend.model.EquipmentStatus;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.model.Role;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.repository.EntityHistoryRepository;
import gov.mib.aims.backend.repository.EquipmentMovementItemRepository;
import gov.mib.aims.backend.repository.EquipmentMovementRepository;
import gov.mib.aims.backend.repository.EquipmentRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.services.mapping.EquipmentMapper;
import gov.mib.aims.backend.services.equipment.EquipmentMovementWorkflow;
import gov.mib.aims.backend.services.mapping.EquipmentMovementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.retry.annotation.Retryable;

import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Учет заявок и независимых снимков их состояния.
 */
@Service
@RequiredArgsConstructor
public class EquipmentMovementServiceImpl implements EquipmentMovementService {

    private final EquipmentMovementRepository movementRepository;
    private final EquipmentMovementItemRepository itemRepository;
    private final EquipmentRepository equipmentRepository;
    private final IncidentRepository incidentRepository;
    private final AppUserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final EntityHistoryRepository historyRepository;
    private final EntityHistoryService historyService;
    private final ObjectMapperWrapper objectMapper;
    private final EquipmentMapper equipmentMapper;
    private final EquipmentMovementMapper mapper;
    private final EquipmentMovementWorkflow workflow;

    @Override
    @Transactional(readOnly = true)
    public EquipmentMovementListResponse list(Long incidentId, Long responsibleUserId,
                                             EquipmentMovementStatusApi status, EquipmentMovementOperationApi operation,
                                             Long equipmentId, int page, int size) {
        var result = movementRepository.search(incidentId, responsibleUserId, status == null ? null : status.name(),
                operation == null ? null : operation.name(), equipmentId, PageRequest.of(page, size));

        return new EquipmentMovementListResponse().items(toResponses(result.getContent()))
                .page(result.getNumber()).size(result.getSize()).totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages());
    }

    @Override
    @Retryable(interceptor = "retryInterceptor")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public EquipmentMovementResponse create(SaveEquipmentMovementRequest request) {
        assertIncidentOpen(request.getIncidentId());
        validate(request);

        EquipmentMovementEntity entity = new EquipmentMovementEntity();
        entity.setOperation(EquipmentMovementOperation.valueOf(request.getOperation().name()));
        entity.setStatus(EquipmentMovementStatus.PENDING);
        entity.setIncidentId(request.getIncidentId());
        entity.setSourceIssueMovementId(request.getSourceIssueMovementId());
        entity.setCreatedByUserId(currentUserService.getCurrentUserId());
        applyChanges(entity, request);

        movementRepository.saveAndFlush(entity);
        saveItems(entity.getId(), request.getEquipmentIds());

        return recordChange(entity, null);
    }

    @Override
    @Retryable(interceptor = "retryInterceptor")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public EquipmentMovementResponse update(Long id, SaveEquipmentMovementRequest request) {
        EquipmentMovementEntity entity = getMutableMovement(id);
        assertAuthorOrStorekeeper(entity);
        if (entity.getStatus() != EquipmentMovementStatus.PENDING) {
            throw Errors.validationError("Изменять можно только заявку в ожидании");
        }

        if (!entity.getIncidentId().equals(request.getIncidentId())
                || !entity.getOperation().name().equals(request.getOperation().name())
                || !Objects.equals(entity.getSourceIssueMovementId(), request.getSourceIssueMovementId())) {
            throw Errors.validationError("Операция, инцидент и исходная выдача заявки не изменяются");
        }

        validate(request);

        applyChanges(entity, request);

        itemRepository.deleteByMovementId(id);
        saveItems(id, request.getEquipmentIds());

        return recordChange(entity, null);
    }

    private void applyChanges(EquipmentMovementEntity entity, SaveEquipmentMovementRequest request) {
        entity.setResponsibleUserId(request.getResponsibleUserId());
        entity.setComment(request.getComment() == null ? null : request.getComment().trim());
    }

    @Override
    @Retryable(interceptor = "retryInterceptor")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public EquipmentMovementResponse changeStatus(Long id, ChangeEquipmentMovementStatusRequest request) {
        EquipmentMovementEntity entity = getMutableMovement(id);
        String comment = request.getComment() == null ? null : request.getComment().trim();
        workflow.changeStatus(entity, EquipmentMovementStatus.valueOf(request.getStatus().name()), comment);

        return recordChange(entity, comment);
    }

    private void saveItems(Long movementId, List<Long> equipmentIds) {
        itemRepository.saveAll(equipmentIds.stream().map(id -> {
            EquipmentMovementItemEntity item = new EquipmentMovementItemEntity();
            item.setMovementId(movementId);
            item.setEquipmentItemId(id);

            return item;
        }).toList());
    }

    private void validate(SaveEquipmentMovementRequest request) {
        List<Long> ids = request.getEquipmentIds();
        if (ids.isEmpty() || ids.contains(null) || new HashSet<>(ids).size() != ids.size()) {
            throw Errors.validationError("Выберите непустой список разных экземпляров оборудования");
        }

        if (!userRepository.existsById(request.getResponsibleUserId())) {
            throw Errors.userNotFound();
        }

        List<EquipmentEntity> equipment = equipmentRepository.findAllById(ids);
        if (equipment.size() != ids.size()) {
            throw Errors.equipmentNotFound();
        }

        if (request.getOperation() == EquipmentMovementOperationApi.ISSUE) {
            if (request.getSourceIssueMovementId() != null) {
                throw Errors.validationError("Исходная выдача указывается только для возврата");
            }

            if (equipment.stream().anyMatch(item -> item.getStatus() != EquipmentStatus.AVAILABLE)) {
                throw Errors.equipmentConflict("Выбранное оборудование уже выдано");
            }
        } else {
            validateReturn(request, equipment);
        }
    }

    private void validateReturn(SaveEquipmentMovementRequest request, List<EquipmentEntity> equipment) {
        if (request.getSourceIssueMovementId() == null) {
            throw Errors.validationError("Для возврата требуется исходная выдача");
        }

        EquipmentMovementEntity issue = movementRepository.findById(request.getSourceIssueMovementId())
                .orElseThrow(Errors::equipmentMovementNotFound);
        if (issue.getOperation() != EquipmentMovementOperation.ISSUE
                || issue.getStatus() != EquipmentMovementStatus.COMPLETED
                || !issue.getIncidentId().equals(request.getIncidentId())
                || !issue.getResponsibleUserId().equals(request.getResponsibleUserId())) {
            throw Errors.validationError("Возврат должен соответствовать выполненной выдаче и ее ответственному");
        }

        if (equipment.stream().anyMatch(item -> item.getStatus() != EquipmentStatus.ISSUED
                || !Objects.equals(item.getCurrentIssueMovementId(), issue.getId())
                || !Objects.equals(item.getHolderUserId(), issue.getResponsibleUserId()))) {
            throw Errors.equipmentConflict("Выбранное оборудование не числится по исходной выдаче");
        }
    }

    private void assertIncidentOpen(Long id) {
        var incident = incidentRepository.findById(id).orElseThrow(Errors::incidentNotFound);
        if (incident.getStatus() == IncidentStatus.EXECUTION_COMPLETED) {
            throw Errors.equipmentConflict("Движение оборудования недоступно для завершенного инцидента");
        }
    }

    private EquipmentMovementEntity getMutableMovement(Long id) {
        EquipmentMovementEntity entity = movementRepository.findById(id)
                .orElseThrow(Errors::equipmentMovementNotFound);
        assertIncidentOpen(entity.getIncidentId());

        return entity;
    }

    private void assertAuthorOrStorekeeper(EquipmentMovementEntity entity) {
        if (!entity.getCreatedByUserId().equals(currentUserService.getCurrentUserId())
                && !currentUserService.hasAnyRole(Role.STOREKEEPER, Role.ADMIN)) {
            throw Errors.accessDenied();
        }
    }

    private EquipmentMovementResponse recordChange(EquipmentMovementEntity entity, String comment) {
        EquipmentMovementResponse response = toResponses(List.of(entity)).get(0);

        historyService.recordChange(EntityType.EQUIPMENT_MOVEMENT, entity.getId(),
                new EquipmentMovementHistorySnapshot().movement(response).transitionComment(comment));

        return response;
    }

    private List<EquipmentMovementResponse> toResponses(List<EquipmentMovementEntity> movements) {
        if (movements.isEmpty()) {
            return List.of();
        }

        var items = itemRepository.findAllByMovementIdInOrderByIdAsc(
                movements.stream().map(EquipmentMovementEntity::getId).toList());
        Map<Long, EquipmentResponse> equipment = equipmentRepository.findAllById(
                        items.stream().map(EquipmentMovementItemEntity::getEquipmentItemId).distinct().toList())
                .stream().map(equipmentMapper::toResponse)
                .collect(Collectors.toMap(EquipmentResponse::getId, Function.identity()));

        Map<Long, List<EquipmentMovementItemEntity>> byMovement = items.stream()
                .collect(Collectors.groupingBy(EquipmentMovementItemEntity::getMovementId));

        return movements.stream().map(movement -> mapper.toResponse(movement,
                byMovement.getOrDefault(movement.getId(), List.of()).stream()
                        .map(item -> equipment.get(item.getEquipmentItemId())).toList())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentMovementResponse getById(Long id) {
        return toResponses(List.of(movementRepository.findById(id)
                .orElseThrow(Errors::equipmentMovementNotFound))).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentMovementHistoryListResponse listHistory(Long id, int page, int size) {
        if (!movementRepository.existsById(id)) {
            throw Errors.equipmentMovementNotFound();
        }

        var result = historyRepository.findByEntityTypeAndEntityIdOrderByChangedAtAsc(
                EntityType.EQUIPMENT_MOVEMENT, id, PageRequest.of(page, size, Sort.by("id")));

        return new EquipmentMovementHistoryListResponse().items(result.map(entry ->
                        new EquipmentMovementHistoryEntry().id(entry.getId())
                                .changedAt(entry.getChangedAt().atOffset(ZoneOffset.UTC))
                                .changedByUserId(entry.getChangedByUserId()).snapshot(objectMapper.readValue(
                                        entry.getSnapshot(), EquipmentMovementHistorySnapshot.class))).getContent())
                .page(result.getNumber()).size(result.getSize()).totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages());
    }
}
