package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.exception.BaseException;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.generated.model.UpdateIncidentRequest;
import gov.mib.aims.backend.model.IncidentEventType;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.incident.IncidentDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Создание и изменение используют одинаковые правила текста, UTC и точности.
 */
class IncidentInputMapperTest {

    private final ChronoService chrono = new ChronoService(Clock.systemUTC());
    private final IncidentInputMapper mapper = new IncidentInputMapper(new IncidentMapper(chrono), chrono);
    private final OffsetDateTime time = OffsetDateTime.parse("2025-05-01T15:34:56.123456789+03:00");

    @Test
    void normalizesCreateAndUpdateToSameDetails() {
        var create = new CreateIncidentRequest().eventType(IncidentEventTypeApi.ALIEN_ARTIFACT)
                .location(" Nevada ").description(" Contact ").detectedAt(time).victimCount(2);
        var update = new UpdateIncidentRequest().eventType(IncidentEventTypeApi.ALIEN_ARTIFACT)
                .location(" Nevada ").description(" Contact ").detectedAt(time).victimCount(2);
        var expected = new IncidentDetails(IncidentEventType.ALIEN_ARTIFACT, "Nevada",
                LocalDateTime.parse("2025-05-01T12:34:56.123456"), "Contact", 2);

        assertThat(mapper.from(create)).isEqualTo(expected);
        assertThat(mapper.from(update)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"location", "description"})
    void rejectsBlankRequiredTextForBothCommands(String field) {
        String location = "location".equals(field) ? " \t " : "Nevada";
        String description = "description".equals(field) ? " \t " : "Contact";
        var create = new CreateIncidentRequest().eventType(IncidentEventTypeApi.ALIEN_ARTIFACT)
                .location(location).description(description).detectedAt(time);
        var update = new UpdateIncidentRequest().eventType(IncidentEventTypeApi.ALIEN_ARTIFACT)
                .location(location).description(description).detectedAt(time);

        assertThatThrownBy(() -> mapper.from(create)).isInstanceOf(BaseException.class);
        assertThatThrownBy(() -> mapper.from(update)).isInstanceOf(BaseException.class);
    }
}
