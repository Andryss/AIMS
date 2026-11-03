package gov.mib.aims.backend.bootstrap;

import gov.mib.aims.backend.controller.BaseApiTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Повторяемая инициализация демо-пользователей и связанных сценариев.
 */
@SpringBootTest(properties = "aims.demo-data.enabled=true")
class DemoDataInitializerTest extends BaseApiTest {

    @Autowired
    private DemoDataInitializer initializer;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void initializesAllRolesAndRelatedDemoScenariosInEmptyDatabase() {
        clearDatabase();

        initializer.run(new DefaultApplicationArguments());

        assertThat(jdbcTemplate.queryForList("""
                select r.name from role r where not exists (
                    select 1 from user_role ur where ur.role_id=r.id)
                """, String.class)).isEmpty();
        assertThat(appUserRepository.findAll()).allSatisfy(user ->
                assertThat(passwordEncoder.matches(user.getLogin(), user.getPasswordHash())).isTrue());
        assertThat(jdbcTemplate.queryForList("select distinct status from tender", String.class))
                .contains("NEED", "COLLECTING_OFFERS", "AWAITING_DELIVERY", "COMPLETED", "CANCELLED");
        assertThat(jdbcTemplate.queryForList("select distinct status from equipment_item", String.class))
                .contains("AVAILABLE", "ISSUED");
        assertThat(jdbcTemplate.queryForList("select distinct status from equipment_movement", String.class))
                .contains("PENDING", "COMPLETED", "CANCELLED");
        assertThat(jdbcTemplate.queryForList("select distinct status from incident", String.class))
                .contains("DRAFT", "READY_FOR_ANALYSIS", "READY_FOR_EXECUTION", "EXECUTING");
        assertThat(jdbcTemplate.queryForObject("select count(*) from incident_comment", Integer.class)).isPositive();
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from equipment_item e join tender t on t.id=e.source_tender_id
                where t.status='COMPLETED' and e.status='AVAILABLE'
                """, Integer.class)).isPositive();
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from entity_history where entity_type='EQUIPMENT'
                and snapshot->>'event'='RECEIVED'
                """, Integer.class)).isPositive();
    }

    @Test
    void repeatedInitializationKeepsPasswordsAndDoesNotDuplicateDemoData() {
        clearDatabase();
        initializer.run(new DefaultApplicationArguments());
        String hash = passwordEncoder.encode("custom-password");
        jdbcTemplate.update("update app_user set password_hash=? where login='tender_manager'", hash);
        Map<String, Object> before = counts();

        initializer.run(new DefaultApplicationArguments());
        initializer.run(new DefaultApplicationArguments());

        assertThat(counts()).isEqualTo(before);
        assertThat(appUserRepository.findByLogin("tender_manager").orElseThrow().getPasswordHash()).isEqualTo(hash);
    }

    @Test
    void addsDetentionScenariosToExistingDemoAndPreservesProgress() {
        clearDatabase();
        initializer.run(new DefaultApplicationArguments());
        String marker = "[DEMO] Задержание у северного терминала";
        jdbcTemplate.update("delete from entity_history where entity_type='DETENTION_REQUEST'");
        jdbcTemplate.update("delete from detention_request");
        jdbcTemplate.update("delete from incident_comment where incident_id in"
                + " (select id from incident where description=?)", marker);
        jdbcTemplate.update("delete from entity_history where entity_type='INCIDENT' and entity_id in"
                + " (select id from incident where description=?)", marker);
        jdbcTemplate.update("delete from incident where description=?", marker);
        String hash = passwordEncoder.encode("guard-password");
        jdbcTemplate.update("update app_user set password_hash=? where login='prison_guard'", hash);

        initializer.run(new DefaultApplicationArguments());

        assertThat(jdbcTemplate.queryForList("select distinct status from detention_request", String.class))
                .containsExactlyInAnyOrder("NEW", "ACCEPTED", "IN_CUSTODY", "RELEASED", "REJECTED", "CANCELLED");
        assertThat(jdbcTemplate.queryForObject("select count(*) from detention_request", Integer.class)).isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject("select count(*) from detention_request"
                + " where status='IN_CUSTODY' and admitted_at is not null and released_at is null", Integer.class))
                .isEqualTo(1);
        jdbcTemplate.update("update detention_request set subject_description='Пользовательское уточнение'"
                + " where status='NEW'");
        Map<String, Object> before = counts();

        initializer.run(new DefaultApplicationArguments());
        initializer.run(new DefaultApplicationArguments());

        assertThat(counts()).isEqualTo(before);
        assertThat(appUserRepository.findByLogin("prison_guard").orElseThrow().getPasswordHash()).isEqualTo(hash);
        assertThat(jdbcTemplate.queryForObject("select subject_description from detention_request where status='NEW'",
                String.class)).isEqualTo("Пользовательское уточнение");
    }

    @Test
    void addsMissingDemoUsersWhenOtherUsersAlreadyExist() {
        clearDatabase();
        String hash = passwordEncoder.encode("existing-password");
        jdbcTemplate.update("insert into app_user(login,password_hash) values ('operator',?)", hash);
        jdbcTemplate.update("insert into alien(name,description,threat_level) values ('Существующий вид','Описание',2)");

        initializer.run(new DefaultApplicationArguments());

        assertThat(appUserRepository.findByLogin("storekeeper")).isPresent();
        assertThat(appUserRepository.findByLogin("tender_manager")).isPresent();
        assertThat(appUserRepository.findByLogin("operator").orElseThrow().getPasswordHash()).isEqualTo(hash);
        assertThat(jdbcTemplate.queryForObject("select count(*) from alien where name='Существующий вид'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void assignmentHistoryUsesAnActorWhoCanAssignAgents() {
        clearDatabase();
        initializer.run(new DefaultApplicationArguments());

        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from entity_history h join app_user u on u.id=h.changed_by_user_id
                where h.entity_type='INCIDENT' and h.snapshot->>'responsibleUserId' is not null
                    and u.login='operator'
                """, Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEMO-BOOT-N-001", "DEMO-BOOT-N-002", "DEMO-BOOT-T-001", "DEMO-BOOT-N-004"})
    void occupiedDemoSerialDoesNotChangeExistingEquipmentOrPreventInitialization(String serial) {
        clearDatabase();
        Long existing = jdbcTemplate.queryForObject("""
                insert into equipment_item(type,model,serial_number,status)
                values ('NEUTRALIZER','Существующий экземпляр',?,'AVAILABLE') returning id
                """, Long.class, serial.toLowerCase(Locale.ROOT));
        Map<String, Object> original = jdbcTemplate.queryForMap("select * from equipment_item where id=?", existing);

        initializer.run(new DefaultApplicationArguments());
        Map<String, Object> seeded = counts();
        initializer.run(new DefaultApplicationArguments());

        assertThat(counts()).isEqualTo(seeded);
        assertThat(jdbcTemplate.queryForMap("select * from equipment_item where id=?", existing)).isEqualTo(original);
        assertThat(jdbcTemplate.queryForObject("select count(*) from equipment_item", Integer.class)).isEqualTo(5);
    }

    private void clearDatabase() {
        jdbcTemplate.execute("TRUNCATE app_user, role, permission, alien, queue_tasks RESTART IDENTITY CASCADE");
    }

    private Map<String, Object> counts() {
        return jdbcTemplate.queryForMap("""
                select (select count(*) from app_user) as users, (select count(*) from role_permission) as grants,
                    (select count(*) from incident) as incidents, (select count(*) from incident_comment) as comments,
                    (select count(*) from equipment_item) as equipment,
                    (select count(*) from equipment_movement) as movements, (select count(*) from tender) as tenders,
                    (select count(*) from tender_participant) as participants,
                    (select count(*) from detention_request) as detention_requests,
                    (select count(*) from entity_history) as history
                """);
    }
}
