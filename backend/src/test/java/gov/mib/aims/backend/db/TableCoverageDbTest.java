package gov.mib.aims.backend.db;

import gov.mib.aims.backend.BaseDbTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Покрытие тестами таблиц даталогической модели")
class TableCoverageDbTest extends BaseDbTest {

    private static final String TABLE_SUITE_PREFIX = "Тестирование таблицы ";
    private static final Set<String> SERVICE_TABLES = Set.of("databasechangelog", "databasechangeloglock", "queue_tasks");
    private static final String QUARTZ_TABLE_PREFIX = "qrtz_";

    @Test
    @DisplayName("[Схема] Для каждой прикладной таблицы написан тест")
    void shouldCoverEveryApplicationTable() {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name
                  FROM information_schema.tables
                 WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                """, String.class);
        Set<String> testedTables = findTestedTables();

        List<String> uncoveredTables = tables.stream()
                .filter(table -> !isServiceTable(table))
                .filter(table -> !testedTables.contains(table))
                .sorted()
                .toList();

        assertThat(uncoveredTables).as("Прикладные таблицы без DB-тестов").isEmpty();
    }

    private Set<String> findTestedTables() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseDbTest.class));

        return scanner.findCandidateComponents(TableCoverageDbTest.class.getPackageName()).stream()
                .map(AnnotatedBeanDefinition.class::cast)
                .map(AnnotatedBeanDefinition::getMetadata)
                .filter(metadata -> metadata.getClassName().endsWith("DbTest"))
                .filter(metadata -> metadata.hasAnnotatedMethods(Testable.class.getName()))
                .map(metadata -> metadata.getAnnotations().get(DisplayName.class))
                .filter(MergedAnnotation::isPresent)
                .map(annotation -> annotation.getString("value"))
                .filter(displayName -> displayName.startsWith(TABLE_SUITE_PREFIX))
                .map(displayName -> displayName.substring(TABLE_SUITE_PREFIX.length()))
                .collect(Collectors.toSet());
    }

    private static boolean isServiceTable(String table) {
        String name = table.toLowerCase(Locale.ROOT);
        return SERVICE_TABLES.contains(name) || name.startsWith(QUARTZ_TABLE_PREFIX);
    }
}
