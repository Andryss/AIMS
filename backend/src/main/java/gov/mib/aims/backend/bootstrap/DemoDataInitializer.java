package gov.mib.aims.backend.bootstrap;

import gov.mib.aims.backend.entity.AppUserEntity;
import gov.mib.aims.backend.entity.PermissionEntity;
import gov.mib.aims.backend.entity.RoleEntity;
import gov.mib.aims.backend.generated.model.ChangeEquipmentMovementStatusRequest;
import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.ChangeTenderStatusRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentCommentRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.EquipmentMovementOperationApi;
import gov.mib.aims.backend.generated.model.EquipmentMovementStatusApi;
import gov.mib.aims.backend.generated.model.EquipmentTypeApi;
import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.generated.model.IncidentStatusApi;
import gov.mib.aims.backend.generated.model.LinkIncidentAlienRequest;
import gov.mib.aims.backend.generated.model.ReceiveTenderEquipmentRequest;
import gov.mib.aims.backend.generated.model.ReceiveTenderRequest;
import gov.mib.aims.backend.generated.model.SaveEquipmentMovementRequest;
import gov.mib.aims.backend.generated.model.SaveEquipmentRequest;
import gov.mib.aims.backend.generated.model.SaveTenderParticipantRequest;
import gov.mib.aims.backend.generated.model.SaveTenderRequest;
import gov.mib.aims.backend.generated.model.SelectTenderWinnerRequest;
import gov.mib.aims.backend.generated.model.SetIncidentExecutorsRequest;
import gov.mib.aims.backend.generated.model.SetIncidentResponsibleRequest;
import gov.mib.aims.backend.generated.model.TenderStatusApi;
import gov.mib.aims.backend.model.Permission;
import gov.mib.aims.backend.model.Role;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.repository.PermissionRepository;
import gov.mib.aims.backend.repository.RoleRepository;
import gov.mib.aims.backend.security.UserInfo;
import gov.mib.aims.backend.services.EquipmentMovementService;
import gov.mib.aims.backend.services.EquipmentService;
import gov.mib.aims.backend.services.IncidentCommentService;
import gov.mib.aims.backend.services.IncidentService;
import gov.mib.aims.backend.services.TenderParticipantService;
import gov.mib.aims.backend.services.TenderReceiptService;
import gov.mib.aims.backend.services.TenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Пользователи всех ролей и связанные демо-сценарии для локальной демонстрации.
 */
@Component
@ConditionalOnProperty(prefix = "aims.demo-data", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements ApplicationRunner {

    private static final String DEMO_INCIDENT = "[DEMO] Светящийся объект у обсерватории";
    private static final Map<String, Role> DEMO_USERS = new LinkedHashMap<>();

    static {
        DEMO_USERS.put("operator", Role.OPERATOR);
        DEMO_USERS.put("analyst", Role.ANALYST);
        DEMO_USERS.put("agent", Role.AGENT);
        DEMO_USERS.put("agent2", Role.AGENT);
        DEMO_USERS.put("storekeeper", Role.STOREKEEPER);
        DEMO_USERS.put("tender_manager", Role.TENDER_MANAGER);
        DEMO_USERS.put("cleaner", Role.CLEANER);
        DEMO_USERS.put("admin", Role.ADMIN);
    }

    private final AppUserRepository appUserRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final IncidentService incidentService;
    private final IncidentCommentService commentService;
    private final EquipmentService equipmentService;
    private final EquipmentMovementService movementService;
    private final TenderService tenderService;
    private final TenderParticipantService participantService;
    private final TenderReceiptService receiptService;
    private final Clock clock;

    @Override
    @Retryable(interceptor = "retryInterceptor")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void run(ApplicationArguments args) {
        Map<String, PermissionEntity> permissionsByCode = seedPermissions();
        Map<String, RoleEntity> rolesByName = seedRoles();
        linkRolePermissions(permissionsByCode, rolesByName);
        seedUsers(rolesByName);
        seedAliens();

        if (!jdbcTemplate.queryForObject("SELECT EXISTS (SELECT 1 FROM incident WHERE description=?)",
                Boolean.class, DEMO_INCIDENT)) {
            seedScenarios();
        }

        log.info("Demo data ready: users {}", DEMO_USERS.keySet());
    }

    private Map<String, PermissionEntity> seedPermissions() {
        Map<String, PermissionEntity> result = new LinkedHashMap<>();
        permissionRepository.findAll().forEach(entity -> result.put(entity.getCode(), entity));
        Arrays.stream(Permission.values()).forEach(permission -> result.computeIfAbsent(permission.getCode(),
                code -> permissionRepository.save(PermissionEntity.builder()
                        .code(code).description(permission.getDescription()).build())));

        return result;
    }

    private Map<String, RoleEntity> seedRoles() {
        Map<String, RoleEntity> result = new LinkedHashMap<>();
        roleRepository.findAll().forEach(entity -> result.put(entity.getName(), entity));
        Arrays.stream(Role.values()).forEach(role -> result.computeIfAbsent(role.getCode(),
                name -> roleRepository.save(RoleEntity.builder()
                        .name(name).description(role.getDescription()).build())));

        return result;
    }

    private void linkRolePermissions(Map<String, PermissionEntity> permissionsByCode,
                                   Map<String, RoleEntity> rolesByName) {
        Arrays.stream(Role.values()).forEach(role -> {
            Long roleId = rolesByName.get(role.getCode()).getId();
            for (Permission permission : role.getPermissions()) {
                jdbcTemplate.update("""
                        INSERT INTO role_permission (role_id, permission_id) VALUES (?, ?)
                        ON CONFLICT DO NOTHING
                        """, roleId, permissionsByCode.get(permission.getCode()).getId());
            }
        });
    }

    private void seedUsers(Map<String, RoleEntity> rolesByName) {
        DEMO_USERS.forEach((login, role) -> {
            AppUserEntity user = appUserRepository.findByLogin(login).orElseGet(() ->
                    appUserRepository.save(AppUserEntity.builder()
                            .login(login).passwordHash(passwordEncoder.encode(login)).build()));
            jdbcTemplate.update("""
                    INSERT INTO user_role (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING
                    """, user.getId(), rolesByName.get(role.getCode()).getId());
        });
    }

    private void seedAliens() {
        jdbcTemplate.update("""
                INSERT INTO alien (name, description, threat_level)
                SELECT demo.name, demo.description, demo.threat_level FROM (VALUES
                    ('Слизень', 'Небольшое слизистое существо', 3),
                    ('Слизистый червь', 'Крупный червеобразный инопланетянин', 6)
                ) AS demo(name, description, threat_level)
                WHERE NOT EXISTS (SELECT 1 FROM alien WHERE name=demo.name)
                """);
    }

    private void seedScenarios() {
        seedIncident(DEMO_INCIDENT, "Обсерватория", IncidentStatusApi.DRAFT);
        seedIncident("[DEMO] Контакт в городском парке", "Городской парк", IncidentStatusApi.READY_FOR_ANALYSIS);
        Long incidentId = seedIncident("[DEMO] Артефакт на складе", "Склад портовой службы",
                IncidentStatusApi.READY_FOR_EXECUTION);
        seedEquipment(incidentId);
        seedIncident("[DEMO] Локализация объекта завершена, требуется очистка", "Старая станция метро",
                IncidentStatusApi.EXECUTING);

        seedTender(TenderStatusApi.NEED, EquipmentTypeApi.NEURALYZER, 2, "[DEMO] Резерв для выездной группы");
        seedTender(TenderStatusApi.COLLECTING_OFFERS, EquipmentTypeApi.NEURALYZER, 2, "[DEMO] Пополнение склада");
        seedTender(TenderStatusApi.AWAITING_DELIVERY, EquipmentTypeApi.NEUTRALIZER, 1,
                "[DEMO] Обновление нейтрализатора");
        seedTender(TenderStatusApi.COMPLETED, EquipmentTypeApi.NEURALYZER, 1, "[DEMO] Поставка учебного оборудования");
        seedTender(TenderStatusApi.CANCELLED, EquipmentTypeApi.NEUTRALIZER, 1, "[DEMO] Закупка отменена");
    }

    private Long seedIncident(String description, String location, IncidentStatusApi target) {
        var incident = asUser("operator", () -> incidentService.create(new CreateIncidentRequest()
                .eventType(IncidentEventTypeApi.ALIEN_ARTIFACT).location(location).description(description)
                .detectedAt(OffsetDateTime.now(clock).minusHours(2)).attachmentFileIds(List.of())));
        asUser("operator", () -> commentService.create(incident.getId(), new CreateIncidentCommentRequest()
                .text("Демонстрационные сведения. Материалы зарегистрированы оператором.")));
        if (target == IncidentStatusApi.DRAFT) {
            return incident.getId();
        }

        asUser("operator", () -> incidentService.changeStatus(incident.getId(), new ChangeIncidentStatusRequest()
                .status(IncidentStatusApi.READY_FOR_ANALYSIS).comment("Передано аналитикам для классификации.")));
        if (target == IncidentStatusApi.READY_FOR_ANALYSIS) {
            return incident.getId();
        }

        Long alienId = jdbcTemplate.queryForObject("SELECT id FROM alien WHERE name='Слизень' ORDER BY id LIMIT 1",
                Long.class);
        asUser("analyst", () -> incidentService.linkAlien(incident.getId(), new LinkIncidentAlienRequest()
                .alienId(alienId)));
        asUser("analyst", () -> incidentService.changeStatus(incident.getId(), new ChangeIncidentStatusRequest()
                .status(IncidentStatusApi.READY_FOR_EXECUTION).comment("Вид определён, требуется выезд группы.")));
        asUser("agent", () -> incidentService.setResponsible(incident.getId(), new SetIncidentResponsibleRequest()
                .userId(userId("agent"))));
        asUser("agent", () -> incidentService.setExecutors(incident.getId(), new SetIncidentExecutorsRequest()
                .userIds(List.of(userId("agent"), userId("agent2")))));

        if (target == IncidentStatusApi.EXECUTING) {
            asUser("agent", () -> incidentService.changeStatus(incident.getId(), new ChangeIncidentStatusRequest()
                    .status(IncidentStatusApi.PREPARATION_FOR_EXECUTION)
                    .comment("Оперативная группа готовится к выезду.")));
            asUser("agent", () -> incidentService.changeStatus(incident.getId(), new ChangeIncidentStatusRequest()
                    .status(IncidentStatusApi.PREPARED_FOR_EXECUTION).comment("Группа и оборудование подготовлены.")));
            asUser("agent", () -> incidentService.changeStatus(incident.getId(), new ChangeIncidentStatusRequest()
                    .status(IncidentStatusApi.EXECUTING).comment("Выполняется операция, можно начать очистку.")));
        }

        return incident.getId();
    }

    private void seedEquipment(Long incidentId) {
        var first = asUser("storekeeper", () -> equipmentService.create(new SaveEquipmentRequest()
                .type(EquipmentTypeApi.NEURALYZER).model("Нейралайзер Н-1")
                .serialNumber(unusedSerial("DEMO-BOOT-N-001"))));
        var second = asUser("storekeeper", () -> equipmentService.create(new SaveEquipmentRequest()
                .type(EquipmentTypeApi.NEURALYZER).model("Нейралайзер Н-2")
                .serialNumber(unusedSerial("DEMO-BOOT-N-002"))));
        var third = asUser("storekeeper", () -> equipmentService.create(new SaveEquipmentRequest()
                .type(EquipmentTypeApi.NEUTRALIZER).model("Нейтрализатор Т-1")
                .serialNumber(unusedSerial("DEMO-BOOT-T-001"))));

        var issued = asUser("storekeeper", () -> movementService.create(new SaveEquipmentMovementRequest()
                .operation(EquipmentMovementOperationApi.ISSUE).incidentId(incidentId)
                .responsibleUserId(userId("agent"))
                .addEquipmentIdsItem(first.getId()).comment("Демо: выдача оборудования оперативной группе.")));
        asUser("storekeeper", () -> movementService.changeStatus(issued.getId(),
                new ChangeEquipmentMovementStatusRequest().status(EquipmentMovementStatusApi.PROCESSING)));
        asUser("storekeeper", () -> movementService.changeStatus(issued.getId(),
                new ChangeEquipmentMovementStatusRequest().status(EquipmentMovementStatusApi.COMPLETED)));
        asUser("agent", () -> movementService.create(new SaveEquipmentMovementRequest()
                .operation(EquipmentMovementOperationApi.ISSUE).incidentId(incidentId)
                .responsibleUserId(userId("agent"))
                .addEquipmentIdsItem(second.getId()).comment("Демо: дополнительный экземпляр, ожидает выдачи.")));
        var cancelled = asUser("storekeeper", () -> movementService.create(new SaveEquipmentMovementRequest()
                .operation(EquipmentMovementOperationApi.ISSUE).incidentId(incidentId)
                .responsibleUserId(userId("agent"))
                .addEquipmentIdsItem(third.getId()).comment("Демо: отменённая выдача.")));
        asUser("storekeeper", () -> movementService.changeStatus(cancelled.getId(),
                new ChangeEquipmentMovementStatusRequest().status(EquipmentMovementStatusApi.CANCELLED)
                        .comment("Оборудование осталось в резерве склада.")));
    }

    private void seedTender(TenderStatusApi target, EquipmentTypeApi type, int quantity, String conditions) {
        var tender = asUser("storekeeper", () -> tenderService.create(new SaveTenderRequest()
                .equipmentType(type).quantity(quantity).conditions(conditions)));
        if (target == TenderStatusApi.NEED) {
            return;
        }

        asUser("tender_manager", () -> tenderService.changeStatus(tender.getId(), new ChangeTenderStatusRequest()
                .status(TenderStatusApi.COLLECTING_OFFERS)));
        if (target == TenderStatusApi.CANCELLED) {
            asUser("tender_manager", () -> tenderService.changeStatus(tender.getId(), new ChangeTenderStatusRequest()
                    .status(TenderStatusApi.CANCELLED).comment("Закупка больше не требуется.")));
            return;
        }

        var participant = asUser("tender_manager", () -> participantService.create(tender.getId(),
                new SaveTenderParticipantRequest().name("ООО Полярис").proposal("Поставка за десять дней.")));
        if (target == TenderStatusApi.COLLECTING_OFFERS) {
            asUser("tender_manager", () -> participantService.create(tender.getId(),
                    new SaveTenderParticipantRequest().name("ООО Сириус").proposal("Поставка за семь дней.")));
            asUser("tender_manager", () -> participantService.update(tender.getId(), participant.getId(),
                    new SaveTenderParticipantRequest().name("ООО Полярис")
                            .proposal("Уточнение: поставка за пять дней.")));
            return;
        }

        asUser("tender_manager", () -> tenderService.selectWinner(tender.getId(), new SelectTenderWinnerRequest()
                .participantId(participant.getId())));
        if (target == TenderStatusApi.COMPLETED) {
            asUser("storekeeper", () -> receiptService.receive(tender.getId(), new ReceiveTenderRequest()
                    .equipment(List.of(new ReceiveTenderEquipmentRequest()
                            .model("Учебный нейралайзер Н-4")
                            .serialNumber(unusedSerial("DEMO-BOOT-N-004"))))
                    .comment("Поставка принята полностью, оборудование доступно на складе.")));
        }
    }

    private String unusedSerial(String base) {
        String candidate = base;
        int suffix = 2;
        while (jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM equipment_item WHERE lower(serial_number)=lower(?))",
                Boolean.class, candidate)) {
            candidate = base + "-" + suffix++;
        }

        return candidate;
    }

    private Long userId(String login) {
        return appUserRepository.findByLogin(login).orElseThrow().getId();
    }

    private <T> T asUser(String login, Supplier<T> action) {
        var user = appUserRepository.findByLogin(login).orElseThrow();
        var previous = SecurityContextHolder.getContext();
        var context = SecurityContextHolder.createEmptyContext();
        var authorities = DEMO_USERS.get(login).getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getCode())).toList();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                new UserInfo(user.getId(), login), null, authorities));
        SecurityContextHolder.setContext(context);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
