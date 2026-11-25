package gov.mib.aims.backend.services.cleanup;

import com.fasterxml.jackson.databind.JsonNode;
import gov.mib.aims.backend.entity.CleanupReportEntity;
import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.BaseException;
import gov.mib.aims.backend.generated.model.CreateCleanupReportRequest;
import gov.mib.aims.backend.generated.model.CleanupReportResponse;
import gov.mib.aims.backend.model.CleanupStatus;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.repository.CleanupReportRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.services.auth.CurrentUserService;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.cleanup.status.CleanupStatusWorkflow;
import gov.mib.aims.backend.services.incident.IncidentHistoryService;
import gov.mib.aims.backend.services.mapping.CleanupMapper;
import gov.mib.aims.backend.services.mapping.IncidentMapper;
import gov.mib.aims.backend.services.validation.AttachmentValidator;
import gov.mib.aims.backend.support.BaseFunctionalTest;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static gov.mib.aims.backend.support.FunctionalTestFixtures.NOW;
import static gov.mib.aims.backend.support.FunctionalTestFixtures.incident;
import static gov.mib.aims.backend.support.AllureTestDataWriter.equal;
import static gov.mib.aims.backend.support.AllureTestDataWriter.exception;
import static gov.mib.aims.backend.support.AllureTestDataWriter.snapshot;
import static gov.mib.aims.backend.support.AllureTestDataWriter.text;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Feature("Очистка последствий")
@Story("UC4. Создание отчёта об очистке")
@DisplayName("CleanupServiceImpl.createReport")
class CleanupServiceImplTest extends BaseFunctionalTest {

    @Mock private IncidentRepository incidents;
    @Mock private CleanupReportRepository reports;
    @Mock private CurrentUserService currentUser;
    @Mock private IncidentHistoryService history;
    @Mock private CleanupStatusWorkflow workflow;
    @Mock private IncidentMapper incidentMapper;
    @Mock private AttachmentValidator attachments;
    @Mock private CleanupMapper mapper;
    @Mock private ChronoService time;
    @InjectMocks private CleanupServiceImpl service;
    private IncidentEntity entity;
    private JsonNode before;
    private final CreateCleanupReportRequest request = new CreateCleanupReportRequest()
            .description("Следы устранены").attachmentFileIds(List.of(101L, 102L));

    @Test
    @DisplayName("Отчёт сохраняется с автором, вложениями и ссылкой из инцидента")
    @Description("Для исполняемого инцидента создаётся отчёт об очистке с описанием выполненных работ и вложениями.")
    void shouldCreateCleanupReport() {
        // Подготовка исходных данных и поведения зависимостей.
        Allure.parameter("Требование", "SRS 3.1.6.1; SRS 3.1.6.2");
        prepare(IncidentStatus.EXECUTING);
        when(reports.existsByIncidentId(42L)).thenReturn(false);
        when(currentUser.getCurrentUserId()).thenReturn(7L);
        when(reports.save(any())).thenAnswer(call -> {
            CleanupReportEntity saved = call.getArgument(0);
            saved.setId(900L);
            return saved;
        });
        when(incidents.save(any())).thenAnswer(call -> call.getArgument(0));
        when(time.localDateTimeNow()).thenReturn(NOW);
        CleanupReportResponse mapped = new CleanupReportResponse().id(900L);
        when(mapper.toReportResponse(any())).thenReturn(mapped);
        snapshot("Ответы зависимостей", Map.of("existingReport", false, "authorId", 7, "returnedReportId", 900));

        // Вызов проверяемого метода.
        CleanupReportResponse response = act("Создать отчёт об очистке инцидента 42 с вложениями 101 и 102",
                () -> service.createReport(42L, request));

        // Проверки результата, сохранённого состояния и вызовов зависимостей.
        snapshot("Ответ сервиса", response);
        ArgumentCaptor<CleanupReportEntity> reportCaptor = ArgumentCaptor.forClass(CleanupReportEntity.class);
        ArgumentCaptor<IncidentEntity> incidentCaptor = ArgumentCaptor.forClass(IncidentEntity.class);
        Allure.step("Проверить сохранения и историю", () -> {
            verify(attachments, times(1)).assertAllExist(request.getAttachmentFileIds());
            verify(reports, times(1)).save(reportCaptor.capture());
            verify(incidents, times(1)).save(incidentCaptor.capture());
            verify(history, times(1)).record(same(incidentCaptor.getValue()));
            verify(mapper).toReportResponse(same(reportCaptor.getValue()));
        });
        snapshot("Сохранённый отчёт", reportCaptor.getValue());
        CleanupReportEntity report = reportCaptor.getValue();
        IncidentEntity expectedIncident = incident(IncidentStatus.EXECUTING);
        expectedIncident.setCleanupStatus(CleanupStatus.EXECUTION);
        expectedIncident.setCleanupReportId(900L);
        expectedIncident.setUpdatedAt(NOW);
        assertAll(
                () -> equal("Сравнить ID инцидента в отчёте с 42", 42L, report.getIncidentId()),
                () -> equal("Сравнить описание отчёта с запросом", "Следы устранены", report.getDescription()),
                () -> equal("Сравнить вложения отчёта с 101 и 102", List.of(101L, 102L), report.getAttachmentFileIds()),
                () -> equal("Сравнить автора отчёта с текущим пользователем 7", 7L, report.getCreatedByUserId()),
                () -> equal("Сравнить дату отчёта с фиксированным временем", NOW, report.getCreatedAt()),
                () -> equal("Сравнить сохраняемое состояние инцидента с ожидаемым",
                        expectedIncident, incidentCaptor.getValue()),
                () -> Allure.step("Проверить возврат ответа маппера отчёта", () -> assertSame(mapped, response))
        );
    }

    @Test
    @DisplayName("Повторный отчёт об очистке отклоняется")
    @Description("Предпринимается повторное создание отчёта об очистке для инцидента, у которого отчёт уже есть.")
    void shouldRejectDuplicateCleanupReport() {
        // Подготовка исходных данных и поведения зависимостей.
        prepare(IncidentStatus.EXECUTING);
        when(reports.existsByIncidentId(42L)).thenReturn(true);
        snapshot("Ответ зависимости", Map.of("existingReport", true));

        // Вызов проверяемого метода.
        BaseException error = act("Попытаться создать повторный отчёт об очистке инцидента 42",
                () -> assertThrows(BaseException.class, () -> service.createReport(42L, request)));

        // Проверки отказа и отсутствия побочных изменений.
        assertRejected(error, 409, "cleanup_report.already_exists");
    }

    @Test
    @DisplayName("Создание отчёта для черновика отклоняется")
    @Description("Предпринимается создание отчёта об очистке для инцидента в состоянии черновика.")
    void shouldRejectCleanupReportForDraftIncident() {
        // Подготовка исходных данных и поведения зависимостей.
        prepare(IncidentStatus.DRAFT);

        // Вызов проверяемого метода.
        BaseException error = act("Попытаться создать отчёт об очистке инцидента 42 в статусе DRAFT",
                () -> assertThrows(BaseException.class, () -> service.createReport(42L, request)));

        // Проверки отказа и отсутствия побочных изменений.
        assertRejected(error, 400, "cleanup.not_allowed");
    }

    private void prepare(IncidentStatus status) {
        entity = incident(status);
        entity.setCleanupStatus(CleanupStatus.EXECUTION);
        Allure.parameter("Проверяемый метод", "CleanupServiceImpl.createReport");
        before = snapshot("Исходный инцидент", entity);
        snapshot("Запрос создания отчёта", request);
        when(incidents.findById(42L)).thenReturn(Optional.of(entity));
    }

    private <T> T act(String actionName, Supplier<T> action) {
        return Allure.step(actionName, () -> {
            try {
                return action.get();
            } finally {
                snapshot("Инцидент после действия", entity);
                text("Вызовы зависимостей", mockingDetails(reports).printInvocations()
                        + mockingDetails(incidents).printInvocations() + mockingDetails(history).printInvocations());
            }
        });
    }

    private void assertRejected(BaseException error, int code, String message) {
        exception(error);
        assertAll(
                () -> equal("Сравнить код отказа с " + code, code, error.getCode()),
                () -> equal("Сравнить причину отказа с " + message, message, error.getMessage()),
                () -> equal("Сравнить состояние инцидента после отказа с исходным",
                        before, snapshot("Проверяемое состояние", entity)),
                () -> Allure.step("Проверить отсутствие сохранений", () -> {
                    verify(reports, never()).save(any());
                    verify(incidents, never()).save(any());
                    verifyNoInteractions(history);
                })
        );
    }
}
