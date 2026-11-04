package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.model.IncidentEventType;
import gov.mib.aims.backend.services.ChronoService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет сохранение кодов событий при преобразовании домена и API.
 */
class IncidentMapperTest {

    private final IncidentMapper mapper = new IncidentMapper(new ChronoService(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)));

    @ParameterizedTest
    @EnumSource(IncidentEventType.class)
    void mapsDomainEventTypeToApi(IncidentEventType type) {
        assertThat(mapper.toApiEventType(type).getValue()).isEqualTo(type.name());
    }

    @ParameterizedTest
    @EnumSource(IncidentEventTypeApi.class)
    void mapsApiEventTypeToDomain(IncidentEventTypeApi type) {
        assertThat(mapper.toModelEventType(type).name()).isEqualTo(type.getValue());
    }
}
