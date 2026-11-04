package gov.mib.aims.backend.services.incident;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.model.IncidentEventType;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Нормализованные основные сведения, общие для создания и изменения инцидента.
 */
public record IncidentDetails(IncidentEventType eventType, String location, LocalDateTime detectedAt,
                              String description, Integer victimCount) {

    /**
     * Применяет основные сведения, не затрагивая состояние процесса.
     */
    public void applyTo(IncidentEntity entity) {
        entity.setEventType(eventType);
        entity.setLocation(location);
        entity.setDetectedAt(detectedAt);
        entity.setDescription(description);
        entity.setVictimCount(victimCount);
    }

    /**
     * Проверяет, изменяют ли подготовленные сведения текущее состояние.
     */
    public boolean matches(IncidentEntity entity) {
        return entity.getEventType() == eventType && Objects.equals(entity.getLocation(), location)
                && Objects.equals(entity.getDetectedAt(), detectedAt)
                && Objects.equals(entity.getDescription(), description)
                && Objects.equals(entity.getVictimCount(), victimCount);
    }
}
