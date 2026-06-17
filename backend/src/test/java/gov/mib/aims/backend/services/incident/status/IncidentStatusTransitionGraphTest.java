package gov.mib.aims.backend.services.incident.status;

import gov.mib.aims.backend.BaseDbTest;
import gov.mib.aims.backend.model.IncidentStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Табличные тесты допустимости переходов статуса инцидента.
 */
class IncidentStatusTransitionGraphTest extends BaseDbTest {

    @Autowired
    private IncidentStatusTransitionGraph graph;

    @ParameterizedTest
    @MethodSource("allowedTransitions")
    void allowedTransitions(IncidentStatus from, IncidentStatus to) {
        assertThat(graph.isAllowed(from, to)).isTrue();
        assertThat(graph.getTransition(from, to)).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("forbiddenTransitions")
    void forbiddenTransitions(IncidentStatus from, IncidentStatus to) {
        assertThat(graph.isAllowed(from, to)).isFalse();
        assertThat(graph.getTransition(from, to)).isNull();
    }

    private static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(IncidentStatus.DRAFT, IncidentStatus.READY_FOR_ANALYSIS),
                Arguments.of(IncidentStatus.READY_FOR_ANALYSIS, IncidentStatus.READY_FOR_EXECUTION),
                Arguments.of(IncidentStatus.READY_FOR_ANALYSIS, IncidentStatus.CLARIFICATION_REQUIRED),
                Arguments.of(IncidentStatus.CLARIFICATION_REQUIRED, IncidentStatus.READY_FOR_ANALYSIS),
                Arguments.of(IncidentStatus.READY_FOR_EXECUTION, IncidentStatus.PREPARATION_FOR_EXECUTION),
                Arguments.of(IncidentStatus.PREPARATION_FOR_EXECUTION, IncidentStatus.PREPARED_FOR_EXECUTION),
                Arguments.of(IncidentStatus.READY_FOR_EXECUTION, IncidentStatus.CLARIFICATION_REQUIRED),
                Arguments.of(IncidentStatus.READY_FOR_EXECUTION, IncidentStatus.REANALYSIS_REQUIRED),
                Arguments.of(IncidentStatus.PREPARATION_FOR_EXECUTION, IncidentStatus.CLARIFICATION_REQUIRED),
                Arguments.of(IncidentStatus.PREPARATION_FOR_EXECUTION, IncidentStatus.REANALYSIS_REQUIRED),
                Arguments.of(IncidentStatus.REANALYSIS_REQUIRED, IncidentStatus.READY_FOR_ANALYSIS),
                Arguments.of(IncidentStatus.PREPARED_FOR_EXECUTION, IncidentStatus.EXECUTING),
                Arguments.of(IncidentStatus.EXECUTING, IncidentStatus.EXECUTION_COMPLETED)
        );
    }

    private static Stream<Arguments> forbiddenTransitions() {
        return Stream.of(
                Arguments.of(IncidentStatus.DRAFT, IncidentStatus.READY_FOR_EXECUTION),
                Arguments.of(IncidentStatus.DRAFT, IncidentStatus.EXECUTING),
                Arguments.of(IncidentStatus.READY_FOR_ANALYSIS, IncidentStatus.PREPARED_FOR_EXECUTION),
                Arguments.of(IncidentStatus.EXECUTION_COMPLETED, IncidentStatus.EXECUTING),
                Arguments.of(IncidentStatus.EXECUTING, IncidentStatus.DRAFT),
                Arguments.of(IncidentStatus.CLARIFICATION_REQUIRED, IncidentStatus.READY_FOR_EXECUTION)
        );
    }
}
