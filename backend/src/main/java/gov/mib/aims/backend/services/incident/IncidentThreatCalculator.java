package gov.mib.aims.backend.services.incident;

import gov.mib.aims.backend.model.IncidentEventType;
import gov.mib.aims.backend.model.IncidentThreatAssessment;
import gov.mib.aims.backend.model.IncidentThreatAssessment.Contribution;
import gov.mib.aims.backend.model.IncidentThreatAssessment.Criterion;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Рассчитывает угрозу по виду, событию и количеству пострадавших.
 */
@Component
public class IncidentThreatCalculator {

    private static final int MIN_THREAT_LEVEL = 1;
    private static final int MAX_THREAT_LEVEL = 10;
    private static final int DEFAULT_THREAT_LEVEL = 3;
    private static final int OBSERVATION_ADJUSTMENT = -1;
    private static final int NO_ADJUSTMENT = 0;
    private static final int ELEVATED_EVENT_ADJUSTMENT = 1;
    private static final int DANGEROUS_EVENT_ADJUSTMENT = 2;
    private static final int FEW_VICTIMS_MAX = 2;
    private static final int SEVERAL_VICTIMS_MAX = 9;
    private static final int FEW_VICTIMS_ADJUSTMENT = 1;
    private static final int SEVERAL_VICTIMS_ADJUSTMENT = 2;
    private static final int MANY_VICTIMS_ADJUSTMENT = 3;

    /**
     * Возвращает ограниченную оценку и исходные вклады без изменения данных.
     */
    public IncidentThreatAssessment calculate(
            IncidentEventType eventType, Integer victimCount, Integer alienThreatLevel
    ) {
        int base = alienThreatLevel != null ? alienThreatLevel : DEFAULT_THREAT_LEVEL;
        int eventAdjustment = switch (eventType) {
            case UNIDENTIFIED_SIGHTING -> OBSERVATION_ADJUSTMENT;
            case CONTACT_SUSPECT, ALIEN_ARTIFACT, ALIEN_CAPTURE -> NO_ADJUSTMENT;
            case ILLEGAL_UFO_LANDING, MEMORY_ANOMALY, PROPERTY_DAMAGE -> ELEVATED_EVENT_ADJUSTMENT;
            case ATTACK_ON_PEOPLE, BIOLOGICAL_CONTAMINATION,
                    CRITICAL_INFRASTRUCTURE_DISRUPTION -> DANGEROUS_EVENT_ADJUSTMENT;
        };
        int victimsAdjustment = victimsAdjustment(victimCount);
        int level = Math.max(MIN_THREAT_LEVEL, Math.min(MAX_THREAT_LEVEL, base + eventAdjustment + victimsAdjustment));
        var contributions = List.of(
                new Contribution(alienThreatLevel != null ? Criterion.ALIEN_THREAT : Criterion.DEFAULT_THREAT, base),
                new Contribution(Criterion.EVENT_TYPE, eventAdjustment),
                new Contribution(Criterion.VICTIMS, victimsAdjustment)
        );

        return new IncidentThreatAssessment(level, contributions);
    }

    private int victimsAdjustment(Integer count) {
        if (count == null || count == 0) {
            return NO_ADJUSTMENT;
        }
        if (count <= FEW_VICTIMS_MAX) {
            return FEW_VICTIMS_ADJUSTMENT;
        }
        if (count <= SEVERAL_VICTIMS_MAX) {
            return SEVERAL_VICTIMS_ADJUSTMENT;
        }

        return MANY_VICTIMS_ADJUSTMENT;
    }
}
