package gov.mib.aims.backend.services.cleanup.status;

import gov.mib.aims.backend.BaseDbTest;
import gov.mib.aims.backend.model.CleanupStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Табличные тесты допустимости переходов статуса очистки.
 */
class CleanupStatusTransitionGraphTest extends BaseDbTest {

    @Autowired
    private CleanupStatusTransitionGraph graph;

    @ParameterizedTest
    @MethodSource("allowedTransitions")
    void allowedTransitions(CleanupStatus from, CleanupStatus to) {
        assertThat(graph.isAllowed(from, to)).isTrue();
        assertThat(graph.getTransition(from, to)).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("forbiddenTransitions")
    void forbiddenTransitions(CleanupStatus from, CleanupStatus to) {
        assertThat(graph.isAllowed(from, to)).isFalse();
        assertThat(graph.getTransition(from, to)).isNull();
    }

    private static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(null, CleanupStatus.PREPARATION),
                Arguments.of(CleanupStatus.PREPARATION, CleanupStatus.EXECUTION),
                Arguments.of(CleanupStatus.EXECUTION, CleanupStatus.COMPLETED)
        );
    }

    private static Stream<Arguments> forbiddenTransitions() {
        return Stream.of(
                Arguments.of(null, CleanupStatus.COMPLETED),
                Arguments.of(CleanupStatus.PREPARATION, CleanupStatus.COMPLETED),
                Arguments.of(CleanupStatus.COMPLETED, CleanupStatus.PREPARATION),
                Arguments.of(CleanupStatus.EXECUTION, CleanupStatus.PREPARATION)
        );
    }
}
