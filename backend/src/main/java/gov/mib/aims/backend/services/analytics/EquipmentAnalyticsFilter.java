package gov.mib.aims.backend.services.analytics;

import gov.mib.aims.backend.generated.model.EquipmentTypeApi;

/**
 * Неизменяемые фильтры исторической аналитики склада.
 */
public record EquipmentAnalyticsFilter(EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {

    /**
     * Сохраняет отсутствие фильтра при передаче типа оборудования в SQL.
     */
    public String equipmentTypeName() {
        return equipmentType == null ? null : equipmentType.name();
    }
}
