package gov.mib.aims.backend.services.equipment;

import gov.mib.aims.backend.entity.EquipmentMovementEntity;
import gov.mib.aims.backend.entity.EquipmentMovementItemEntity;
import gov.mib.aims.backend.generated.model.EquipmentMovementResponse;
import gov.mib.aims.backend.generated.model.EquipmentResponse;
import gov.mib.aims.backend.repository.EquipmentMovementItemRepository;
import gov.mib.aims.backend.repository.EquipmentRepository;
import gov.mib.aims.backend.services.mapping.EquipmentMapper;
import gov.mib.aims.backend.services.mapping.EquipmentMovementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Загружает позиции и оборудование пакетами перед сборкой представления заявок.
 */
@Component
@RequiredArgsConstructor
public class EquipmentMovementResponseAssembler {

    private final EquipmentMovementItemRepository itemRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentMapper equipmentMapper;
    private final EquipmentMovementMapper mapper;

    /**
     * Собирает представление одной заявки тем же способом, что и список.
     */
    public EquipmentMovementResponse toResponse(EquipmentMovementEntity movement) {
        return toResponses(List.of(movement)).get(0);
    }

    /**
     * Загружает связанные данные одним пакетом для всей страницы заявок.
     */
    public List<EquipmentMovementResponse> toResponses(List<EquipmentMovementEntity> movements) {
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
}
