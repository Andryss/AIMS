package gov.mib.aims.backend.model;

import gov.mib.aims.backend.exception.BaseException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Тесты {@link EntityRef}.
 */
class EntityRefTest {

    @Test
    void formatAndParseRoundtrip() {
        String ref = EntityRef.format(EntityType.INCIDENT, 42L);
        assertThat(ref).isEqualTo("INCIDENT:42");
        EntityRef parsed = EntityRef.parse(ref);
        assertThat(parsed.entityType()).isEqualTo(EntityType.INCIDENT);
        assertThat(parsed.entityId()).isEqualTo(42L);
    }

    @Test
    void parseInvalidRefThrows() {
        assertThatThrownBy(() -> EntityRef.parse("bad-ref"))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getCode()).isEqualTo(400));
    }
}
