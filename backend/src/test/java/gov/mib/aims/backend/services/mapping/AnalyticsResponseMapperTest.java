package gov.mib.aims.backend.services.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.mib.aims.backend.generated.model.AnalyticsBucketUnitApi;
import gov.mib.aims.backend.generated.model.AnalyticsMeta;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.ObjectMapperWrapper;
import gov.mib.aims.backend.services.analytics.AnalyticsQueryContext;
import gov.mib.aims.backend.services.analytics.AnalyticsReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Типизированные представления аналитики сохраняют данные SQL и получают общую метаинформацию.
 */
class AnalyticsResponseMapperTest {

    private final ChronoService chronoService = new ChronoService(Clock.fixed(
            Instant.parse("2025-06-01T12:00:00.123456789Z"), ZoneId.of("Europe/Kirov")));
    private final AnalyticsResponseMapper mapper = new AnalyticsResponseMapper(
            new ObjectMapperWrapper(new ObjectMapper().findAndRegisterModules()), chronoService);

    @Test
    void mapsCountAndReplacesSqlMetadataWhilePreservingMissingHistory() {
        var response = mapper.toCount(report("\"count\":12"));

        assertThat(response.getCount()).isEqualTo(12L);
        assertMetadata(response.getMeta());
    }

    @Test
    void preservesZeroCounts() {
        var response = mapper.toCount(report("\"count\":0"));

        assertThat(response.getCount()).isZero();
        assertMetadata(response.getMeta());
    }

    @Test
    void preservesEmptyGroups() {
        var response = mapper.toGrouped(report("\"groups\":[]"));

        assertThat(response.getGroups()).isEmpty();
        assertMetadata(response.getMeta());
    }

    @Test
    void preservesZeroTrendBuckets() {
        var response = mapper.toTrend(report("""
                "buckets":[{"from":"2025-05-01T00:00:00Z","to":"2025-05-02T00:00:00Z","values":{"CREATED":0}}]
                """));

        assertThat(response.getBuckets()).singleElement().satisfies(bucket -> {
            assertThat(bucket.getFrom()).isEqualTo(OffsetDateTime.parse("2025-05-01T00:00:00Z"));
            assertThat(bucket.getTo()).isEqualTo(OffsetDateTime.parse("2025-05-02T00:00:00Z"));
            assertThat(bucket.getValues()).containsEntry("CREATED", 0L);
        });
        assertMetadata(response.getMeta());
    }

    @Test
    void preservesDurationSamplesAndMissingPercentiles() {
        var response = mapper.toDuration(report("""
                "percentiles":{"sampleCount":0,"excludedSampleCount":3,"p50Seconds":null,"p90Seconds":null}
                """));

        assertThat(response.getPercentiles().getSampleCount()).isZero();
        assertThat(response.getPercentiles().getExcludedSampleCount()).isEqualTo(3L);
        assertThat(response.getPercentiles().getP50Seconds()).isNull();
        assertThat(response.getPercentiles().getP90Seconds()).isNull();
        assertMetadata(response.getMeta());
    }

    @Test
    void preservesStaleIncidentTotalAndEmptyItems() {
        var response = mapper.toStaleIncident(report("\"totalCount\":0,\"items\":[]"));

        assertThat(response.getTotalCount()).isZero();
        assertThat(response.getItems()).isEmpty();
        assertMetadata(response.getMeta());
    }

    @Test
    void preservesInventoryQuantities() {
        var response = mapper.toEquipmentInventory(report("""
                "items":[{"equipmentType":"NEURALYZER","available":2,"issued":3,"total":5,"issuedShare":0.6}]
                """));

        assertThat(response.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getAvailable()).isEqualTo(2L);
            assertThat(item.getIssued()).isEqualTo(3L);
            assertThat(item.getTotal()).isEqualTo(5L);
            assertThat(item.getIssuedShare()).isEqualTo(0.6);
        });
        assertMetadata(response.getMeta());
    }

    @Test
    void preservesOpenRequestTotalsAndEmptyItems() {
        var response = mapper.toOpenEquipmentRequests(report("\"totalCount\":0,\"items\":[]"));

        assertThat(response.getTotalCount()).isZero();
        assertThat(response.getItems()).isEmpty();
        assertMetadata(response.getMeta());
    }

    @Test
    void preservesExpectedDeliveryQuantities() {
        var response = mapper.toExpectedDeliveries(report("""
                "items":[{"equipmentType":"NEURALYZER","tenderCount":2,"unitCount":17}]
                """));

        assertThat(response.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getTenderCount()).isEqualTo(2L);
            assertThat(item.getUnitCount()).isEqualTo(17L);
        });
        assertMetadata(response.getMeta());
    }

    @Test
    void preservesOutstandingEquipmentTotalsAndEmptyItems() {
        var response = mapper.toOutstandingEquipment(report("\"totalCount\":0,\"items\":[]"));

        assertThat(response.getTotalCount()).isZero();
        assertThat(response.getItems()).isEmpty();
        assertMetadata(response.getMeta());
    }

    @Test
    void preservesUpcomingReleaseTotalsAndEmptyItems() {
        var response = mapper.toUpcomingReleases(report("\"totalCount\":0,\"items\":[]"));

        assertThat(response.getTotalCount()).isZero();
        assertThat(response.getItems()).isEmpty();
        assertMetadata(response.getMeta());
    }

    @ParameterizedTest
    @CsvSource({"2025-05-01T00:00:00Z,2025-05-02T00:00:00Z,HOUR,1",
            "2025-05-01T00:00:00Z,2025-06-01T00:00:00Z,DAY,1",
            "2000-01-01T00:00:00Z,2025-01-01T00:00:00Z,MONTH,3"})
    void mapsInternalBucketUnits(String from, String to, AnalyticsBucketUnitApi unit, int step) {
        var context = AnalyticsQueryContext.of(OffsetDateTime.parse(from), OffsetDateTime.parse(to), chronoService);
        var response = mapper.toCount(new AnalyticsReport("{\"meta\":{\"recordsWithoutSnapshot\":0},\"count\":0}",
                context));

        assertThat(response.getMeta().getBucketUnit()).isEqualTo(unit);
        assertThat(response.getMeta().getBucketStep()).isEqualTo(step);
        assertThat(response.getMeta().getRecordsWithoutSnapshot()).isZero();
    }

    private AnalyticsReport report(String payload) {
        var context = AnalyticsQueryContext.of(OffsetDateTime.parse("2025-05-01T03:00:00+03:00"),
                OffsetDateTime.parse("2025-06-01T03:00:00+03:00"), chronoService);

        return new AnalyticsReport("{\"meta\":{\"recordsWithoutSnapshot\":7,\"bucketStep\":99}," + payload + "}",
                context);
    }

    private void assertMetadata(AnalyticsMeta meta) {
        assertThat(meta.getRecordsWithoutSnapshot()).isEqualTo(7L);
        assertThat(meta.getFrom()).isEqualTo(OffsetDateTime.parse("2025-05-01T00:00:00Z"));
        assertThat(meta.getFrom().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(meta.getTo()).isEqualTo(OffsetDateTime.parse("2025-06-01T00:00:00Z"));
        assertThat(meta.getGeneratedAt()).isEqualTo(OffsetDateTime.parse("2025-06-01T12:00:00.123456Z"));
        assertThat(meta.getBucketUnit()).isEqualTo(AnalyticsBucketUnitApi.DAY);
        assertThat(meta.getBucketStep()).isEqualTo(1);
    }
}
