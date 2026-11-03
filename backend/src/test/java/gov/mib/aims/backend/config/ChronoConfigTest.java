package gov.mib.aims.backend.config;

import gov.mib.aims.backend.services.ChronoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Конфигурация часов внедряется в единый сервис времени.
 */
class ChronoConfigTest {

    @Test
    void testProfileProvidesFixedClockToChronoService() {
        new ApplicationContextRunner()
                .withUserConfiguration(ChronoConfig.class, TestChronoConfig.class, ChronoService.class)
                .withPropertyValues("spring.profiles.active=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(Clock.class).hasSingleBean(ChronoService.class);
                    assertThat(context.getBean(Clock.class).instant()).isEqualTo(TestChronoConfig.FIXED_INSTANT);
                    assertThat(context.getBean(ChronoService.class).now()).isEqualTo(TestChronoConfig.FIXED_INSTANT);
                });
    }
    @ParameterizedTest
    @ValueSource(strings = {"default", "local", "prod"})
    void otherProfilesProvideSystemUtcClock(String profile) {
        new ApplicationContextRunner()
                .withUserConfiguration(ChronoConfig.class, TestChronoConfig.class, ChronoService.class)
                .withPropertyValues("spring.profiles.active=" + profile)
                .run(context -> {
                    assertThat(context).hasSingleBean(Clock.class).hasSingleBean(ChronoService.class);
                    assertThat(context.getBean(Clock.class)).isEqualTo(Clock.systemUTC());
                    assertThat(context.getBean(ChronoService.class).offsetDateTimeNow().getOffset())
                            .isEqualTo(ZoneOffset.UTC);
                });
    }

}
