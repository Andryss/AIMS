package gov.mib.aims.backend.services.query;

import gov.mib.aims.backend.exception.BaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Единая политика параметров страниц.
 */
class PageParametersTest {

    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,-1", "0,101"})
    void rejectsInvalidPageParameters(int page, int size) {
        assertThatThrownBy(() -> PageParameters.of(page, size)).isInstanceOf(BaseException.class)
                .satisfies(error -> assertThat(((BaseException) error).getCode()).isEqualTo(400));
    }

    @Test
    void retainsEndpointSpecificDefaultsAndValidBoundaries() {
        assertThat(PageParameters.of(null, null)).isEqualTo(new PageParameters(0, 20));
        assertThat(PageParameters.of(null, null, 50)).isEqualTo(new PageParameters(0, 50));
        assertThat(PageParameters.of(2, 100).toPageable()).isEqualTo(PageRequest.of(2, 100));
    }

}
