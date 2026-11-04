package gov.mib.aims.backend.services.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.mib.aims.backend.services.ChronoService;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Контракт метаданных одинаков для всех явных мапперов списков и истории.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PageResponseMappersTest {

    private final ChronoService chrono = new ChronoService(Clock.systemUTC());
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @ParameterizedTest(name = "{0}")
    @MethodSource("responses")
    void retainsRequestedPageAndTotalsForEmptyResults(String name, Supplier<Object> factory) {
        JsonNode response = json.valueToTree(factory.get());

        assertThat(response.path("items").isArray()).isTrue();
        assertThat(response.path("items").isEmpty()).isTrue();
        assertThat(response.path("page").asInt()).isEqualTo(2);
        assertThat(response.path("size").asInt()).isEqualTo(2);
        assertThat(response.path("totalElements").asLong()).isEqualTo(9);
        assertThat(response.path("totalPages").asInt()).isEqualTo(5);
    }

    Stream<Arguments> responses() {
        var incidents = new IncidentMapper(chrono);
        var equipment = new EquipmentMapper(chrono);
        var movements = new EquipmentMovementMapper(chrono);
        var detentions = new DetentionRequestMapper(chrono);
        var tenders = new TenderMapper(chrono);
        var incidentHistory = new IncidentSnapshotMapper(incidents, chrono);

        return Stream.of(
                response("incidents", () -> incidents.toListResponse(page())),
                response("aliens", () -> new AlienMapper().toListResponse(page())),
                response("equipment", () -> equipment.toListResponse(page())),
                response("movements", () -> movements.toListResponse(page(), List.of())),
                response("detentions", () -> detentions.toListResponse(page())),
                response("tenders", () -> tenders.toListResponse(page(), Map.of())),
                response("participants", () -> new TenderParticipantMapper(chrono).toListResponse(page())),
                response("comments", () -> new IncidentCommentMapper(chrono).toListResponse(page())),
                response("notifications", () -> new NotificationMapper(chrono).toListResponse(page())),
                response("monitoring", () -> new MonitoringAlertMapper(chrono, incidents).toListResponse(page())),
                response("incident history", () -> incidentHistory.toHistoryListResponse(page())),
                response("equipment history", () -> equipment.toHistoryListResponse(page())),
                response("movement history", () -> movements.toHistoryListResponse(page())),
                response("detention history", () -> detentions.toHistoryListResponse(page())),
                response("tender history", () -> tenders.toHistoryListResponse(page())));
    }

    private Arguments response(String name, Supplier<Object> factory) {
        return Arguments.of(name, factory);
    }

    private <T> Page<T> page() {
        return new PageImpl<>(List.of(), PageRequest.of(2, 2), 9);
    }
}
