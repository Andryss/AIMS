package gov.mib.aims.backend.services.incident;

import gov.mib.aims.backend.model.IncidentEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет правила оценки угрозы и ограничение доменной шкалой.
 */
class IncidentThreatCalculatorTest {

    private final IncidentThreatCalculator calculator = new IncidentThreatCalculator();

    @ParameterizedTest
    @CsvSource({"UNIDENTIFIED_SIGHTING,-1,2", "CONTACT_SUSPECT,0,3", "ILLEGAL_UFO_LANDING,1,4",
            "MEMORY_ANOMALY,1,4", "ALIEN_ARTIFACT,0,3", "ALIEN_CAPTURE,0,3", "ATTACK_ON_PEOPLE,2,5",
            "PROPERTY_DAMAGE,1,4", "BIOLOGICAL_CONTAMINATION,2,5", "CRITICAL_INFRASTRUCTURE_DISRUPTION,2,5"})
    void appliesEventAdjustment(IncidentEventType eventType, int adjustment, int expected) {
        var result = calculator.calculate(eventType, 0, null);

        assertThat(result.calculatedThreatLevel()).isEqualTo(expected);
        assertThat(result.contributions().get(1).adjustment()).isEqualTo(adjustment);
    }

    @ParameterizedTest
    @CsvSource({"0,0,3", "1,1,4", "2,1,4", "3,2,5", "9,2,5", "10,3,6", "2147483647,3,6"})
    void appliesVictimCountBands(int count, int adjustment, int expected) {
        var result = calculator.calculate(IncidentEventType.CONTACT_SUSPECT, count, null);

        assertThat(result.calculatedThreatLevel()).isEqualTo(expected);
        assertThat(result.contributions().get(2).adjustment()).isEqualTo(adjustment);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0})
    void usesDefaultBaseWithNoVictimsInformation(Integer victims) {
        var result = calculator.calculate(IncidentEventType.CONTACT_SUSPECT, victims, null);

        assertThat(result.calculatedThreatLevel()).isEqualTo(3);
        assertThat(result.contributions()).extracting(c -> c.criterion().name())
                .containsExactly("DEFAULT_THREAT", "EVENT_TYPE", "VICTIMS");
        assertThat(result.contributions()).extracting(c -> c.adjustment()).containsExactly(3, 0, 0);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 7, 9, 10})
    void usesSpeciesThreatAcrossDomainScale(int base) {
        var result = calculator.calculate(IncidentEventType.CONTACT_SUSPECT, null, base);

        assertThat(result.calculatedThreatLevel()).isEqualTo(base);
    }

    @ParameterizedTest
    @CsvSource({"UNIDENTIFIED_SIGHTING,1,0,1", "ATTACK_ON_PEOPLE,10,10,10"})
    void clampsSumToDomainScale(IncidentEventType type, int base, int victims, int expected) {
        var result = calculator.calculate(type, victims, base);

        assertThat(result.calculatedThreatLevel()).isEqualTo(expected);
    }

    @Test
    void explainsSevenAsFourPlusTwoPlusOne() {
        var result = calculator.calculate(IncidentEventType.ATTACK_ON_PEOPLE, 2, 4);

        assertThat(result.calculatedThreatLevel()).isEqualTo(7);
        assertThat(result.contributions()).extracting(c -> c.criterion().name())
                .containsExactly("ALIEN_THREAT", "EVENT_TYPE", "VICTIMS");
        assertThat(result.contributions()).extracting(c -> c.adjustment()).containsExactly(4, 2, 1);
    }
}
