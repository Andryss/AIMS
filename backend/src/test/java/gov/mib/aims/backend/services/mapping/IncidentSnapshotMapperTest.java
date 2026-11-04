package gov.mib.aims.backend.services.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.generated.model.CleanupStatusApi;
import gov.mib.aims.backend.generated.model.IncidentStatusApi;
import gov.mib.aims.backend.model.CleanupStatus;
import gov.mib.aims.backend.model.IncidentEventType;
import gov.mib.aims.backend.model.IncidentSnapshot;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.services.ChronoService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Совместимость явных снимков с плоским JSON прежней JPA-сущности.
 */
class IncidentSnapshotMapperTest {

    private final ChronoService chrono = new ChronoService(Clock.systemUTC());
    private final IncidentSnapshotMapper mapper = new IncidentSnapshotMapper(new IncidentMapper(chrono), chrono);
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void preservesEveryLegacyStorageFieldAndCopiesMutableLists() {
        LocalDateTime time = LocalDateTime.parse("2025-05-01T12:34:56.123456");
        var entity = IncidentEntity.builder().id(42L).status(IncidentStatus.EXECUTING)
                .eventType(IncidentEventType.ALIEN_CAPTURE).location("Nevada").description("Contact")
                .detectedAt(time).victimCount(2).threatLevel(7)
                .attachmentFileIds(new ArrayList<>(List.of(10L, 11L))).alienId(3L)
                .cleanupStatus(CleanupStatus.EXECUTION).cleanupReportId(5L).responsibleUserId(6L)
                .executorUserIds(new ArrayList<>(List.of(6L, 7L))).createdByUserId(8L).monitoringAlertId(9L)
                .createdAt(time).updatedAt(time).build();

        var snapshot = mapper.toStorage(entity);

        JsonNode stored = json.valueToTree(snapshot);
        JsonNode legacy = json.valueToTree(entity);
        assertThat(stored).isEqualTo(legacy);
        entity.getAttachmentFileIds().clear();
        entity.getExecutorUserIds().clear();
        assertThat(snapshot.attachmentFileIds()).containsExactly(10L, 11L);
        assertThat(snapshot.executorUserIds()).containsExactly(6L, 7L);
    }

    @Test
    void readsOldSnapshotsWithNullableOptionalFields() throws Exception {
        String legacy = """
                {"id":42,"status":"EXECUTING","eventType":"ALIEN_CAPTURE","location":"Nevada",
                 "detectedAt":"2025-05-01T12:34:56.123456","description":"Contact","victimCount":null,
                 "threatLevel":7,"attachmentFileIds":[10],"alienId":3,"cleanupStatus":"EXECUTION",
                 "cleanupReportId":5,"responsibleUserId":6,"executorUserIds":null,"createdByUserId":8,
                 "monitoringAlertId":null,"createdAt":"2025-05-01T00:00:00","updatedAt":"2025-05-01T12:34:56"}
                """;

        var response = mapper.toResponse(json.readValue(legacy, IncidentSnapshot.class));

        assertThat(response.getStatus()).isEqualTo(IncidentStatusApi.EXECUTING);
        assertThat(response.getCleanupStatus()).isEqualTo(CleanupStatusApi.EXECUTION);
        assertThat(response.getDetectedAt().toString()).isEqualTo("2025-05-01T12:34:56.123456Z");
        assertThat(response.getVictimCount()).isNull();
        assertThat(response.getExecutorUserIds()).isEmpty();
        assertThat(response.getAttachmentFileIds()).containsExactly(10L);
        assertThat(response.getThreatLevel()).isEqualTo(7);
    }
}
