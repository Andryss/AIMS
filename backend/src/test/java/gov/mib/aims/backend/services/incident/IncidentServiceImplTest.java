package gov.mib.aims.backend.services.incident;

import com.fasterxml.jackson.databind.JsonNode;
import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.BaseException;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.IncidentStatusApi;
import gov.mib.aims.backend.generated.model.IncidentResponse;
import gov.mib.aims.backend.generated.model.LinkIncidentAlienRequest;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.repository.AlienRepository;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.repository.MonitoringAlertRepository;
import gov.mib.aims.backend.services.auth.CurrentUserService;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.incident.status.IncidentStatusWorkflow;
import gov.mib.aims.backend.services.mapping.IncidentInputMapper;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Feature("Инциденты")
@DisplayName("IncidentServiceImpl")
class IncidentServiceImplTest extends BaseFunctionalTest {

    @Mock private IncidentRepository incidents;
    @Mock private MonitoringAlertRepository alerts;
    @Mock private AlienRepository aliens;
    @Mock private AppUserRepository users;
    @Mock private CurrentUserService currentUser;
    @Mock private IncidentHistoryService history;
    @Mock private IncidentInputMapper inputMapper;
    @Mock private IncidentStatusWorkflow workflow;
    @Mock private IncidentCommentService comments;
    @Mock private ExecutorAssignmentNotifier notifier;
    @Mock private AttachmentValidator attachments;
    @Mock private IncidentMapper mapper;
    @Mock private ChronoService time;
    @InjectMocks private IncidentServiceImpl service;
    private IncidentEntity entity;
    private JsonNode before;

    @Test
    @Story("UC2. Классификация инопланетянина")
    @DisplayName("Существующий вид привязывается к готовому к анализу инциденту")
    @Description("К инциденту, готовому к анализу, привязывается выбранный вид из справочника.")
    void shouldLinkAlienForIncidentReadyForAnalysis() {
        // Подготовка исходных данных и поведения зависимостей.
        Allure.parameter("Требование", "SRS 3.1.2.4");
        prepare(IncidentStatus.READY_FOR_ANALYSIS, "IncidentServiceImpl.linkAlien");
        LinkIncidentAlienRequest request = new LinkIncidentAlienRequest().alienId(100L);
        snapshot("Запрос привязки", request);
        when(aliens.existsById(100L)).thenReturn(true);
        when(incidents.save(any())).thenAnswer(call -> call.getArgument(0));
        when(time.localDateTimeNow()).thenReturn(NOW);
        IncidentResponse mapped = new IncidentResponse().id(42L);
        when(mapper.toResponse(any())).thenReturn(mapped);

        // Вызов проверяемого метода.
        IncidentResponse response = act("Привязать вид 100 к инциденту 42 в статусе READY_FOR_ANALYSIS",
                () -> service.linkAlien(42L, request));

        // Проверки результата, сохранённого состояния и вызовов зависимостей.
        snapshot("Ответ сервиса", response);
        ArgumentCaptor<IncidentEntity> captured = ArgumentCaptor.forClass(IncidentEntity.class);
        Allure.step("Проверить сохранение и историю", () -> {
            verify(incidents, times(1)).save(captured.capture());
            verify(history, times(1)).record(same(captured.getValue()));
            verify(mapper).toResponse(same(captured.getValue()));
        });
        IncidentEntity expected = incident(IncidentStatus.READY_FOR_ANALYSIS);
        expected.setAlienId(100L);
        expected.setUpdatedAt(NOW);
        assertAll(
                () -> equal("Сравнить сохраняемые бизнес-поля с ожидаемым инцидентом", expected, captured.getValue()),
                () -> Allure.step("Проверить возврат ответа маппера", () -> assertSame(mapped, response))
        );
    }

    @Test
    @Story("UC2. Классификация инопланетянина")
    @DisplayName("Привязка вида отклоняется для черновика")
    @Description("Предпринимается привязка вида к инциденту, который ещё находится в состоянии черновика.")
    void shouldRejectAlienLinkForDraftIncident() {
        // Подготовка исходных данных и поведения зависимостей.
        prepare(IncidentStatus.DRAFT, "IncidentServiceImpl.linkAlien");
        LinkIncidentAlienRequest request = new LinkIncidentAlienRequest().alienId(100L);
        snapshot("Запрос привязки", request);

        // Вызов проверяемого метода.
        BaseException error = act("Попытаться привязать вид 100 к инциденту 42 в статусе DRAFT",
                () -> assertThrows(BaseException.class, () -> service.linkAlien(42L, request)));

        // Проверки отказа и отсутствия побочных изменений.
        assertRejected(error, 400, "incident.invalid_alien_link");
        Allure.step("Проверить отсутствие поиска вида", () -> verifyNoInteractions(aliens));
    }

    @Test
    @Story("UC2. Классификация инопланетянина")
    @DisplayName("Привязка неизвестного вида отклоняется")
    @Description("К инциденту, готовому к анализу, пытаются привязать отсутствующий в справочнике вид.")
    void shouldRejectLinkToUnknownAlien() {
        // Подготовка исходных данных и поведения зависимостей.
        Allure.parameter("Требование", "SRS 3.1.2.4");
        prepare(IncidentStatus.READY_FOR_ANALYSIS, "IncidentServiceImpl.linkAlien");
        LinkIncidentAlienRequest request = new LinkIncidentAlienRequest().alienId(100L);
        snapshot("Запрос привязки", request);
        when(aliens.existsById(100L)).thenReturn(false);

        // Вызов проверяемого метода.
        BaseException error = act("Попытаться привязать неизвестный вид 100 к инциденту 42",
                () -> assertThrows(BaseException.class, () -> service.linkAlien(42L, request)));

        // Проверки отказа и отсутствия побочных изменений.
        assertRejected(error, 404, "alien.not_found");
    }

    @Test
    @Story("UC3. Назначение ответственных")
    @DisplayName("Новый статус сохраняется перед историей и post-action")
    @Description("Подготовленный инцидент переводится в готовность к выполнению с обработкой уведомлений.")
    void shouldSaveStatusBeforeHistoryAndPostAction() {
        // Подготовка исходных данных и поведения зависимостей.
        Allure.parameter("Требование", "SRS 3.1.1.2");
        prepare(IncidentStatus.PREPARATION_FOR_EXECUTION, "IncidentServiceImpl.changeStatus");
        IncidentEntity changed = incident(IncidentStatus.PREPARED_FOR_EXECUTION);
        changed.setUpdatedAt(NOW);
        IncidentEntity saved = incident(IncidentStatus.PREPARED_FOR_EXECUTION);
        saved.setUpdatedAt(NOW.plusNanos(1_000));
        String comment = "Готов к выполнению";
        ChangeIncidentStatusRequest request = new ChangeIncidentStatusRequest().status(IncidentStatusApi.PREPARED_FOR_EXECUTION)
                .comment(comment);
        snapshot("Запрос смены статуса", request);
        snapshot("Результат, возвращаемый сохранением", saved);
        when(mapper.toModelStatus(IncidentStatusApi.PREPARED_FOR_EXECUTION))
                .thenReturn(IncidentStatus.PREPARED_FOR_EXECUTION);
        when(workflow.changeStatus(entity, IncidentStatus.PREPARED_FOR_EXECUTION, comment)).thenReturn(changed);
        when(incidents.save(changed)).thenReturn(saved);
        IncidentResponse mapped = new IncidentResponse().id(42L);
        when(mapper.toResponse(saved)).thenReturn(mapped);

        // Вызов проверяемого метода.
        IncidentResponse response = act("Перевести инцидент 42 из PREPARATION_FOR_EXECUTION в PREPARED_FOR_EXECUTION",
                () -> service.changeStatus(42L, request));

        // Проверки результата, порядка сохранения и вызовов зависимостей.
        snapshot("Ответ сервиса", response);
        ArgumentCaptor<IncidentEntity> captured = ArgumentCaptor.forClass(IncidentEntity.class);
        Allure.step("Проверить порядок save → история → post-action", () -> {
            InOrder order = inOrder(incidents, history, workflow);
            order.verify(workflow).changeStatus(entity, IncidentStatus.PREPARED_FOR_EXECUTION, comment);
            order.verify(incidents).save(captured.capture());
            order.verify(history).record(same(saved));
            order.verify(workflow).afterSave(same(saved), eq(IncidentStatus.PREPARATION_FOR_EXECUTION), eq(comment));
            verify(incidents, times(1)).save(changed);
            verify(history, times(1)).record(same(saved));
            verify(workflow, times(1))
                    .afterSave(same(saved), eq(IncidentStatus.PREPARATION_FOR_EXECUTION), eq(comment));
            verify(comments, times(1)).createFromStatusChange(42L, comment);
            verify(mapper).toResponse(same(saved));
        });
        assertAll(
                () -> equal("Сравнить сохраняемый статус с PREPARED_FOR_EXECUTION",
                        IncidentStatus.PREPARED_FOR_EXECUTION,
                        captured.getValue().getStatus()),
                () -> equal("Сравнить сохраняемое время изменения с фиксированным временем",
                        NOW, captured.getValue().getUpdatedAt()),
                () -> Allure.step("Проверить возврат ответа маппера сохранённого инцидента",
                        () -> assertSame(mapped, response))
        );
    }

    @Test
    @Story("UC3. Назначение ответственных")
    @DisplayName("Отклонённый переход останавливает сохранение, историю и уведомление")
    @Description("Предпринимается перевод инцидента в готовность к выполнению без выполнения условий подготовки.")
    void shouldStopStatusChangeWhenWorkflowRejectsTransition() {
        // Подготовка исходных данных и поведения зависимостей.
        prepare(IncidentStatus.PREPARATION_FOR_EXECUTION, "IncidentServiceImpl.changeStatus");
        ChangeIncidentStatusRequest request = new ChangeIncidentStatusRequest().status(IncidentStatusApi.PREPARED_FOR_EXECUTION)
                .comment("Проверить готовность");
        snapshot("Запрос смены статуса", request);
        when(mapper.toModelStatus(IncidentStatusApi.PREPARED_FOR_EXECUTION))
                .thenReturn(IncidentStatus.PREPARED_FOR_EXECUTION);
        when(workflow.changeStatus(entity, IncidentStatus.PREPARED_FOR_EXECUTION, request.getComment()))
                .thenThrow(Errors.validationError("Responsible agent must be assigned"));

        // Вызов проверяемого метода.
        BaseException error = act("Попытаться перевести инцидент 42 в PREPARED_FOR_EXECUTION при отказе workflow",
                () -> assertThrows(BaseException.class, () -> service.changeStatus(42L, request)));

        // Проверки отказа и отсутствия побочных изменений.
        assertRejected(error, 400, "validation.error");
        Allure.step("Проверить отсутствие post-action и комментария", () -> {
            verify(workflow, never()).afterSave(any(), any(), any());
            verifyNoInteractions(comments);
        });
    }

    private void prepare(IncidentStatus status, String method) {
        Allure.parameter("Проверяемый метод", method);
        entity = incident(status);
        before = snapshot("Исходный инцидент", entity);
        when(incidents.findById(42L)).thenReturn(Optional.of(entity));
    }

    private <T> T act(String actionName, Supplier<T> action) {
        return Allure.step(actionName, () -> {
            try {
                return action.get();
            } finally {
                snapshot("Инцидент после действия", entity);
                text("Вызовы зависимостей", mockingDetails(incidents).printInvocations()
                        + mockingDetails(history).printInvocations() + mockingDetails(workflow).printInvocations());
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
                () -> Allure.step("Проверить отсутствие записи", () -> {
                    verify(incidents, never()).save(any());
                    verifyNoInteractions(history);
                })
        );
    }
}
