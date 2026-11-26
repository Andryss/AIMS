package gov.mib.aims.e2e;

import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.e2e.E2EFixtures.Account;
import gov.mib.aims.e2e.E2EFixtures.IncidentInput;
import gov.mib.aims.e2e.pages.IncidentCreateForm;
import gov.mib.aims.e2e.pages.IncidentDetailsPage;
import gov.mib.aims.e2e.pages.IncidentDetailsPage.HistoryEntry;
import gov.mib.aims.e2e.pages.IncidentDetailsPage.State;
import gov.mib.aims.e2e.pages.IncidentsPage;
import gov.mib.aims.e2e.pages.LoginPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static gov.mib.aims.backend.support.AllureTestDataWriter.equal;
import static gov.mib.aims.backend.support.AllureTestDataWriter.snapshot;
import static org.junit.jupiter.api.Assertions.assertAll;

@Feature("UC1. Регистрация инцидента")
class IncidentRegistrationE2ETest extends BaseE2ETest {

    @ParameterizedTest(name = "{displayName} — {0}")
    @MethodSource("browsers")
    @Story("Основной поток — действия оператора")
    @DisplayName("UC1 Оператор регистрирует инцидент и отправляет его на анализ")
    @Description("Оператор регистрирует инцидент с изображением и отправляет его на анализ.")
    void shouldRegisterIncidentAndSendForAnalysis(E2EBrowser browser) {
        // Подготовка окружения и исходных данных.
        Allure.parameter("Связанные требования", List.of("3.1.1.1", "3.1.1.2", "3.1.3.1"));
        startBrowser(browser);
        Account operator = fixtures.account("operator", "operator");
        IncidentInput input = fixtures.incidentInput();
        snapshot("Исходные данные регистрации", input);
        IncidentsPage list = new LoginPage(driver, baseUrl()).signIn(operator);
        Allure.step("Выполнен вход под оператором (" + operator.login() + ")");

        // Действие: регистрация инцидента.
        IncidentCreateForm form = uiStep("Открыть форму регистрации инцидента", list::openCreateForm);
        uiStep("Указать основные сведения об инциденте", () -> form.fill(input));
        uiStep("Прикрепить изображение к инциденту", () -> form.upload(fixtures.imagePath()));
        IncidentsPage savedList = uiStep("Сохранить инцидент", form::save);
        IncidentDetailsPage card = uiStep("Открыть зарегистрированный инцидент", () ->
                savedList.openByLocation(input.location()));
        long incidentId = card.readId();

        // Проверка результата регистрации.
        equal("Начальный статус инцидента", IncidentStatus.DRAFT, card::readStatus);

        // Действие: отправка инцидента на анализ.
        uiStep("Отправить инцидент на анализ", () ->
                card.changeStatus(IncidentStatus.READY_FOR_ANALYSIS));

        // Проверки сохранённых данных и истории изменений.
        uiStep("Вернуться к списку инцидентов", card::backToList);
        State actual = uiStep("Повторно открыть карточку инцидента", () -> {
            card.open(incidentId);
            return card.readState();
        });
        Allure.step("Проверить сохранённые данные регистрации", () -> {
            snapshot("Сохранённая карточка", actual);
            assertAll(
                    () -> equal("Номер инцидента", incidentId, actual.id()),
                    () -> equal("Статус", IncidentStatus.READY_FOR_ANALYSIS, actual.status()),
                    () -> equal("Тип события", "Подозрение на контакт", actual.eventType()),
                    () -> equal("Место", input.location(), actual.location()),
                    () -> equal("Время обнаружения", input.detectedAt(), actual.detectedAt()),
                    () -> equal("Описание", input.description(), actual.description()),
                    () -> equal("Вложение", fixtures.imageName(), actual.attachmentName()),
                    () -> equal("PNG загружен и декодирован браузером", 32, actual.attachmentPreviewWidth())
            );
        });
        List<HistoryEntry> history = uiStep("Открыть историю регистрации инцидента", () -> card.readHistory(2));
        Allure.step("Проверить историю создания и отправки на анализ", () -> {
            snapshot("История инцидента", history);
            assertAll(
                    () -> equal("Количество записей истории", 2, history.size()),
                    () -> equal("Запись создания", true, history.get(0).heading().contains("Создание инцидента")),
                    () -> equal("Исходный статус в истории", "Черновик", history.get(0).changes().get("Статус")),
                    () -> equal("Переход статуса в истории", "Черновик → Готов к анализу",
                            history.get(1).changes().get("Статус")),
                    () -> equal("Автор создания", operator.login(), history.get(0).author()),
                    () -> equal("Автор отправки на анализ", operator.login(), history.get(1).author())
            );
        });
    }
}
