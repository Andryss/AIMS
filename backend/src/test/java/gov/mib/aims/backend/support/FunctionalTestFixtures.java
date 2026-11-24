package gov.mib.aims.backend.support;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.model.IncidentEventType;
import gov.mib.aims.backend.model.IncidentStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Независимые данные с фиксированным бизнес-временем.
 */
public final class FunctionalTestFixtures {

    public static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 9, 0);

    private FunctionalTestFixtures() {
    }

    /** Создаёт новый инцидент с заполненными обязательными бизнес-полями. */
    public static IncidentEntity incident(IncidentStatus status) {
        return IncidentEntity.builder()
                .id(42L)
                .status(status)
                .eventType(IncidentEventType.CONTACT_SUSPECT)
                .location("Москва")
                .detectedAt(NOW.minusDays(2))
                .description("Замечен неизвестный объект")
                .victimCount(0)
                .threatLevel(3)
                .attachmentFileIds(new ArrayList<>(List.of(101L, 102L)))
                .executorUserIds(new ArrayList<>())
                .createdByUserId(5L)
                .createdAt(NOW.minusDays(1))
                .updatedAt(NOW.minusDays(1))
                .build();
    }
}
