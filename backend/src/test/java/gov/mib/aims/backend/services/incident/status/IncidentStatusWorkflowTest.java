package gov.mib.aims.backend.services.incident.status;

import com.fasterxml.jackson.databind.JsonNode;
import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.BaseException;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.incident.IncidentTransitionContext;
import gov.mib.aims.backend.services.status.StatusTransition;
import gov.mib.aims.backend.services.status.StatusTransitionPostAction;
import gov.mib.aims.backend.services.status.StatusTransitionPrecondition;
import gov.mib.aims.backend.support.BaseFunctionalTest;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static gov.mib.aims.backend.support.FunctionalTestFixtures.NOW;
import static gov.mib.aims.backend.support.FunctionalTestFixtures.incident;
import static gov.mib.aims.backend.support.AllureTestDataWriter.equal;
import static gov.mib.aims.backend.support.AllureTestDataWriter.exception;
import static gov.mib.aims.backend.support.AllureTestDataWriter.snapshot;
import static gov.mib.aims.backend.support.AllureTestDataWriter.text;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Feature("Жизненный цикл инцидента")
@DisplayName("IncidentStatusWorkflow")
class IncidentStatusWorkflowTest extends BaseFunctionalTest {

    @Mock private IncidentStatusTransitionGraph graph;
    @Mock private ChronoService time;
    @Mock private StatusTransition<IncidentTransitionContext, IncidentStatus> transition;
    @Mock private StatusTransition<IncidentTransitionContext, IncidentStatus> otherTransition;
    @Mock private StatusTransitionPrecondition<IncidentTransitionContext> precondition;
    @Mock private StatusTransitionPostAction<IncidentTransitionContext> postAction;
    @Mock private StatusTransitionPostAction<IncidentTransitionContext> otherPostAction;
    @InjectMocks private IncidentStatusWorkflow workflow;

    @Test
    @DisplayName("Смена статуса обновляет время, post-action ожидает afterSave")
    @Story("UC2. Классификация инопланетянина")
    @Description("Готовый к анализу инцидент направляется на уточнение с комментарием о необходимых сведениях.")
    void changingStatusMutatesWithoutPersistenceOrNotification() {
        // Подготовка исходных данных и поведения зависимостей.
        IncidentEntity entity = incident(IncidentStatus.READY_FOR_ANALYSIS);
        snapshot("Исходный инцидент", entity);
        allow(IncidentStatus.READY_FOR_ANALYSIS, IncidentStatus.CLARIFICATION_REQUIRED, transition);
        when(time.localDateTimeNow()).thenReturn(NOW);

        // Вызов проверяемого метода.
        act("Перевести инцидент 42 из READY_FOR_ANALYSIS в CLARIFICATION_REQUIRED", entity,
                () -> workflow.changeStatus(entity, IncidentStatus.CLARIFICATION_REQUIRED, "Уточнить сведения"));

        // Проверки состояния и вызовов зависимостей.
        assertAll(
                () -> equal("Сравнить итоговый статус с CLARIFICATION_REQUIRED",
                        IncidentStatus.CLARIFICATION_REQUIRED, entity.getStatus()),
                () -> equal("Сравнить время изменения с фиксированным временем", NOW, entity.getUpdatedAt()),
                () -> Allure.step("Проверить передачу инцидента и комментария в предусловие", () ->
                        verify(precondition).check(new IncidentTransitionContext(entity, "Уточнить сведения"))),
                () -> Allure.step("Проверить отсутствие post-action до afterSave", () ->
                        verifyNoInteractions(postAction, otherPostAction)),
                () -> Allure.step("Проверить отсутствие чтения post-action во время changeStatus", () ->
                        verify(transition, never()).getPostActions())
        );
    }

    @Test
    @DisplayName("Чередующиеся переходы передают post-action собственные комментарии")
    @Stories({@Story("UC2. Классификация инопланетянина"), @Story("UC3. Назначение ответственных")})
    @Description("Инциденты направляются на уточнение и повторный анализ с собственными комментариями.")
    void afterSaveUsesEachIncidentsExplicitCommentWhenTransitionsAreInterleaved() {
        // Подготовка исходных данных и поведения зависимостей.
        IncidentEntity first = incident(IncidentStatus.READY_FOR_ANALYSIS);
        IncidentEntity second = incident(IncidentStatus.READY_FOR_EXECUTION);
        second.setId(43L);
        String firstComment = "  Need a clearer location  ";
        String secondComment = "a".repeat(121);
        snapshot("Исходные инциденты", List.of(first, second));
        snapshot("Комментарии", List.of(firstComment, secondComment));
        allow(IncidentStatus.READY_FOR_ANALYSIS, IncidentStatus.CLARIFICATION_REQUIRED, transition);
        allow(IncidentStatus.READY_FOR_EXECUTION, IncidentStatus.REANALYSIS_REQUIRED, otherTransition);
        when(time.localDateTimeNow()).thenReturn(NOW);
        when(transition.getPostActions()).thenReturn(List.of(postAction));
        when(otherTransition.getPostActions()).thenReturn(List.of(otherPostAction));

        // Вызов смены статуса для двух инцидентов.
        Allure.step("Перевести инцидент 42 в CLARIFICATION_REQUIRED, а инцидент 43 в REANALYSIS_REQUIRED", () -> {
            workflow.changeStatus(first, IncidentStatus.CLARIFICATION_REQUIRED, firstComment);
            workflow.changeStatus(second, IncidentStatus.REANALYSIS_REQUIRED, secondComment);
        });
        // Проверки до вызова afterSave.
        Allure.step("Проверить отсутствие post-action до сохранения", () ->
                verifyNoInteractions(postAction, otherPostAction));

        // Вызов обработки после сохранения в обратном порядке.
        Allure.step("Выполнить afterSave сначала для инцидента 43, затем для инцидента 42", () -> {
            workflow.afterSave(second, IncidentStatus.READY_FOR_EXECUTION, secondComment);
            workflow.afterSave(first, IncidentStatus.READY_FOR_ANALYSIS, firstComment);
        });
        // Проверки порядка вызовов и переданных контекстов.
        ArgumentCaptor<IncidentTransitionContext> firstContext = ArgumentCaptor.forClass(IncidentTransitionContext.class);
        ArgumentCaptor<IncidentTransitionContext> secondContext = ArgumentCaptor.forClass(IncidentTransitionContext.class);
        Allure.step("Проверить порядок вызова post-action и получить переданные контексты", () -> {
            InOrder order = inOrder(postAction, otherPostAction);
            order.verify(otherPostAction).execute(secondContext.capture());
            order.verify(postAction).execute(firstContext.capture());
            order.verifyNoMoreInteractions();
        });
        snapshot("Первый контекст post-action", firstContext.getValue());
        snapshot("Второй контекст post-action", secondContext.getValue());
        assertAll(
                () -> equal("Сравнить ID инцидента уточнения с 42", 42L, firstContext.getValue().incident().getId()),
                () -> equal("Сравнить комментарий уточнения с исходным", firstComment,
                        firstContext.getValue().comment()),
                () -> equal("Сравнить ID инцидента повторного анализа с 43", 43L,
                        secondContext.getValue().incident().getId()),
                () -> equal("Сравнить комментарий повторного анализа с исходным", secondComment,
                        secondContext.getValue().comment())
        );
    }

    @ParameterizedTest(name = "{displayName} — {0}")
    @MethodSource("absentComments")
    @DisplayName("Пустой комментарий передаётся post-action независимо от предыдущего перехода")
    @Story("UC2. Классификация инопланетянина")
    @Description("Последовательно на уточнение направляются инциденты с комментарием и без него.")
    void absentCommentDoesNotReuseAnotherTransitionsComment(String comment) {
        // Подготовка исходных данных и поведения зависимостей.
        Allure.parameter("Комментарий", comment);
        IncidentEntity first = incident(IncidentStatus.READY_FOR_ANALYSIS);
        IncidentEntity second = incident(IncidentStatus.READY_FOR_ANALYSIS);
        second.setId(43L);
        snapshot("Исходные инциденты", List.of(first, second));
        allow(IncidentStatus.READY_FOR_ANALYSIS, IncidentStatus.CLARIFICATION_REQUIRED, transition);
        when(time.localDateTimeNow()).thenReturn(NOW);
        when(transition.getPostActions()).thenReturn(List.of(postAction));

        // Вызов переходов с разными комментариями.
        Allure.step("Выполнить переходы в CLARIFICATION_REQUIRED с разными комментариями", () -> {
            workflow.changeStatus(first, IncidentStatus.CLARIFICATION_REQUIRED, "Previous explanation");
            workflow.afterSave(first, IncidentStatus.READY_FOR_ANALYSIS, "Previous explanation");
            workflow.changeStatus(second, IncidentStatus.CLARIFICATION_REQUIRED, comment);
            workflow.afterSave(second, IncidentStatus.READY_FOR_ANALYSIS, comment);
        });
        // Проверки контекстов, переданных в post-action.
        ArgumentCaptor<IncidentTransitionContext> captured = ArgumentCaptor.forClass(IncidentTransitionContext.class);
        Allure.step("Проверить два вызова post-action и получить переданные контексты", () ->
                verify(postAction, times(2)).execute(captured.capture()));
        snapshot("Переданные контексты", captured.getAllValues());
        IncidentTransitionContext context = captured.getAllValues().get(1);
        assertAll(
                () -> equal("Сравнить ID нового инцидента с 43", 43L, context.incident().getId()),
                () -> equal("Сравнить переданный комментарий с исходным значением", comment, context.comment())
        );
    }

    @Test
    @DisplayName("Неразрешённый переход сохраняет исходное состояние")
    @Story("Допустимые переходы инцидента")
    @Description("Предпринимается перевод черновика сразу в исполнение, минуя обязательные этапы подготовки.")
    void invalidTransitionDoesNotMutateOrNotify() {
        // Подготовка исходных данных и поведения зависимостей.
        IncidentEntity entity = incident(IncidentStatus.DRAFT);
        JsonNode before = snapshot("Исходный инцидент", entity);

        // Вызов проверяемого метода.
        BaseException error = act("Попытаться перевести инцидент 42 из DRAFT в EXECUTING без перехода в графе", entity,
                () -> assertThrows(BaseException.class,
                        () -> workflow.changeStatus(entity, IncidentStatus.EXECUTING, "Invalid")));

        // Проверки отказа и отсутствия побочных изменений.
        assertAll(
                () -> equal("Сравнить код отказа с 400", 400, error.getCode()),
                () -> equal("Сравнить причину отказа с incident.invalid_status_transition",
                        "incident.invalid_status_transition", error.getMessage()),
                () -> equal("Сравнить состояние после отказа с исходным", before,
                        snapshot("Проверяемое состояние", entity)),
                () -> Allure.step("Проверить отсутствие вызовов часов, предусловия и post-action", () ->
                        verifyNoInteractions(time, precondition, postAction, otherPostAction))
        );
    }

    @Test
    @DisplayName("Успешное предусловие позволяет подготовить инцидент")
    @Story("UC3. Назначение ответственных")
    @Description("Инцидент с назначенными ответственным и исполнителями переводится в готовность к выполнению.")
    void shouldPrepareAssignedIncident() {
        // Подготовка исходных данных и поведения зависимостей.
        IncidentEntity entity = incident(IncidentStatus.PREPARATION_FOR_EXECUTION);
        entity.setResponsibleUserId(7L);
        entity.setExecutorUserIds(new ArrayList<>(List.of(8L)));
        snapshot("Исходный инцидент", entity);
        allow(IncidentStatus.PREPARATION_FOR_EXECUTION, IncidentStatus.PREPARED_FOR_EXECUTION, transition);
        when(time.localDateTimeNow()).thenReturn(NOW);
        when(transition.getPostActions()).thenReturn(List.of(postAction));

        // Вызов смены статуса.
        act("Перевести инцидент 42 из PREPARATION_FOR_EXECUTION в PREPARED_FOR_EXECUTION", entity,
                () -> workflow.changeStatus(entity, IncidentStatus.PREPARED_FOR_EXECUTION, null));

        // Проверки состояния до вызова afterSave.
        assertAll(
                () -> equal("Сравнить итоговый статус с PREPARED_FOR_EXECUTION",
                        IncidentStatus.PREPARED_FOR_EXECUTION, entity.getStatus()),
                () -> equal("Сравнить время изменения с фиксированным временем", NOW, entity.getUpdatedAt()),
                () -> Allure.step("Проверить вызов предусловия и отсутствие post-action до afterSave", () -> {
                    verify(precondition).check(new IncidentTransitionContext(entity, null));
                    verifyNoInteractions(postAction);
                })
        );
        // Вызов обработки после сохранения.
        Allure.step("Выполнить post-action перехода PREPARATION_FOR_EXECUTION → PREPARED_FOR_EXECUTION", () ->
                workflow.afterSave(entity, IncidentStatus.PREPARATION_FOR_EXECUTION, null));

        // Проверки post-action и переданного контекста.
        ArgumentCaptor<IncidentTransitionContext> captured = ArgumentCaptor.forClass(IncidentTransitionContext.class);
        Allure.step("Проверить вызов post-action и получить переданный контекст", () ->
                verify(postAction).execute(captured.capture()));
        snapshot("Контекст post-action", captured.getValue());
        equal("Сравнить ID инцидента в контексте post-action с 42", 42L, captured.getValue().incident().getId());
    }

    @Test
    @DisplayName("Ошибка предусловия об отсутствующем ответственном сохраняет исходное состояние")
    @Story("UC3. Назначение ответственных")
    @Description("Предпринимается подготовка инцидента к выполнению без назначенного ответственного.")
    void shouldRejectPreparationWithoutResponsible() {
        // Подготовка исходных данных и поведения зависимостей.
        IncidentEntity entity = incident(IncidentStatus.PREPARATION_FOR_EXECUTION);
        entity.setExecutorUserIds(new ArrayList<>(List.of(8L)));
        String message = "Responsible agent must be assigned";
        JsonNode before = prepareRejection(entity, message);

        // Вызов проверяемого метода.
        BaseException error = rejectPreparation(entity);

        // Проверки отказа и отсутствия побочных изменений.
        assertPreparationRejected(entity, before, error, message);
    }

    @ParameterizedTest(name = "{displayName} — {0}")
    @MethodSource("missingExecutors")
    @DisplayName("Ошибка предусловия об исполнителях сохраняет исходное состояние")
    @Story("UC3. Назначение ответственных")
    @Description("Предпринимается подготовка инцидента к выполнению без назначенных исполнителей.")
    void shouldRejectPreparationWithoutExecutors(List<Long> executorIds) {
        // Подготовка исходных данных и поведения зависимостей.
        Allure.parameter("Исполнители", executorIds);
        IncidentEntity entity = incident(IncidentStatus.PREPARATION_FOR_EXECUTION);
        entity.setResponsibleUserId(7L);
        entity.setExecutorUserIds(executorIds);
        String message = "At least one executor must be assigned";
        JsonNode before = prepareRejection(entity, message);

        // Вызов проверяемого метода.
        BaseException error = rejectPreparation(entity);

        // Проверки отказа и отсутствия побочных изменений.
        assertPreparationRejected(entity, before, error, message);
    }

    private JsonNode prepareRejection(IncidentEntity entity, String humanMessage) {
        JsonNode before = snapshot("Исходный инцидент", entity);
        when(graph.getTransition(IncidentStatus.PREPARATION_FOR_EXECUTION, IncidentStatus.PREPARED_FOR_EXECUTION))
                .thenReturn(transition);
        when(transition.getPreconditions()).thenReturn(List.of(precondition));
        doThrow(Errors.validationError(humanMessage)).when(precondition).check(any());
        return before;
    }

    private BaseException rejectPreparation(IncidentEntity entity) {
        return act("Попытаться перевести инцидент 42 в PREPARED_FOR_EXECUTION при отказе предусловия", entity,
                () -> assertThrows(BaseException.class,
                        () -> workflow.changeStatus(entity, IncidentStatus.PREPARED_FOR_EXECUTION, null)));
    }

    private void assertPreparationRejected(IncidentEntity entity, JsonNode before, BaseException error,
                                           String humanMessage) {
        exception(error);
        assertAll(
                () -> equal("Сравнить код отказа с 400", 400, error.getCode()),
                () -> equal("Сравнить причину отказа с validation.error", "validation.error", error.getMessage()),
                () -> equal("Сравнить описание отказа с ошибкой предусловия", humanMessage, error.getHumanMessage()),
                () -> equal("Сравнить состояние после отказа с исходным", before,
                        snapshot("Проверяемое состояние", entity)),
                () -> Allure.step("Проверить отсутствие вызовов часов и post-action после отказа", () ->
                        verifyNoInteractions(time, postAction)),
                () -> Allure.step("Проверить отсутствие чтения post-action после отказа предусловия", () ->
                        verify(transition, never()).getPostActions())
        );
    }

    private void allow(IncidentStatus from, IncidentStatus to,
                       StatusTransition<IncidentTransitionContext, IncidentStatus> descriptor) {
        when(graph.getTransition(from, to)).thenReturn(descriptor);
        when(descriptor.getPreconditions()).thenReturn(List.of(precondition));
        when(descriptor.getTo()).thenReturn(to);
    }

    private <T> T act(String actionName, IncidentEntity entity, Supplier<T> action) {
        Allure.parameter("Проверяемые методы", "IncidentStatusWorkflow.changeStatus / afterSave");
        return Allure.step(actionName, () -> {
            try {
                return action.get();
            } finally {
                snapshot("Инцидент после перехода", entity);
                text("Вызовы post-action", mockingDetails(postAction).printInvocations());
            }
        });
    }

    private static Stream<Arguments> absentComments() {
        return Stream.of(
                Arguments.of(Named.of("комментарий не задан (null)", (String) null)),
                Arguments.of(Named.of("пустая строка", "")),
                Arguments.of(Named.of("один пробел", " ")),
                Arguments.of(Named.of("табуляция и перевод строки", "\t\n")),
                Arguments.of(Named.of("нулевой символ", "\0"))
        );
    }

    private static Stream<Arguments> missingExecutors() {
        return Stream.of(
                Arguments.of(Named.of("список исполнителей не задан (null)", (List<Long>) null)),
                Arguments.of(Named.of("пустой список исполнителей", List.of()))
        );
    }
}
