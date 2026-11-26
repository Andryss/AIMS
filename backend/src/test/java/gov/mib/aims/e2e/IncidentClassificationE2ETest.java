package gov.mib.aims.e2e;

import gov.mib.aims.backend.entity.AlienEntity;
import gov.mib.aims.backend.entity.StoredFileEntity;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.e2e.E2EFixtures.Account;
import gov.mib.aims.e2e.E2EFixtures.IncidentInput;
import gov.mib.aims.e2e.E2EFixtures.PreparedIncident;
import gov.mib.aims.e2e.pages.AlienPickerDrawer;
import gov.mib.aims.e2e.pages.IncidentDetailsPage;
import gov.mib.aims.e2e.pages.IncidentDetailsPage.HistoryEntry;
import gov.mib.aims.e2e.pages.IncidentDetailsPage.State;
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

@Feature("UC2. Классификация инопланетянина")
class IncidentClassificationE2ETest extends BaseE2ETest {

    @ParameterizedTest(name = "{displayName} — {0}")
    @MethodSource("browsers")
    @Story("Основной поток — ручной выбор вида")
    @DisplayName("UC2 Аналитик выбирает вид и отправляет инцидент на выполнение")
    @Description("Аналитик выбирает вид из справочника и отправляет инцидент на выполнение.")
    void shouldClassifyIncidentAndSendForExecution(E2EBrowser browser) {
        // Подготовка окружения и исходных данных.
        Allure.parameter("Связанные требования", List.of("3.1.1.2", "3.1.2.3", "3.1.2.4"));
        startBrowser(browser);
        Account operator = fixtures.account("operator", "operator");
        Account analyst = fixtures.account("analyst", "analyst");
        IncidentInput input = fixtures.incidentInput();
        AlienEntity alien = fixtures.alien("Слизень");
        StoredFileEntity image = fixtures.storeFile(operator, fixtures.imagePath(), "image/png");
        PreparedIncident prepared = fixtures.createIncident(operator, IncidentStatus.READY_FOR_ANALYSIS,
                input, browserZone, List.of(image.getId()));
        long incidentId = prepared.id();
        snapshot("Исходное состояние для классификации", prepared);

        // Подготовка авторизованной сессии.
        new LoginPage(driver, baseUrl()).signIn(analyst);
        Allure.step("Выполнен вход под аналитиком (" + analyst.login() + ")");

        // Действие: открытие инцидента для анализа.
        IncidentDetailsPage card = new IncidentDetailsPage(driver, baseUrl());
        State initial = uiStep("Открыть инцидент без классификации", () -> {
            card.open(incidentId);
            return card.readState();
        });

        // Проверка исходного состояния.
        snapshot("Исходная карточка инцидента", initial);
        equal("Исходный статус", IncidentStatus.READY_FOR_ANALYSIS, initial.status());
        equal("Классификация ещё не задана", null, initial.alienName());

        // Действие: ручная классификация инцидента.
        AlienPickerDrawer picker = uiStep("Открыть дровер выбора вида", card::openAlienPicker);
        uiStep("Ввести название вида и получить результаты поиска", () -> picker.search(alien.getName()));
        uiStep("Выбрать найденный вид", () -> picker.select(alien.getName()));
        String selectedAlien = uiStep("Подтвердить выбор вида и вернуться к инциденту", () -> {
            picker.confirm();
            return card.readAlienName();
        });

        // Проверка результата выбора вида.
        equal("Выбранный вид", alien.getName(), selectedAlien);

        // Действие: отправка инцидента на выполнение.
        uiStep("Отправить инцидент на выполнение", () ->
                card.changeStatus(IncidentStatus.READY_FOR_EXECUTION));

        // Проверки сохранённых данных и истории изменений.
        uiStep("Вернуться к списку инцидентов", card::backToList);
        State actual = uiStep("Повторно открыть инцидент с сохранённой классификацией", () -> {
            card.open(incidentId);
            card.readAlienName();
            return card.readState();
        });
        Allure.step("Проверить сохранённые данные классификации", () -> {
            snapshot("Карточка после классификации", actual);
            assertAll(
                    () -> equal("Номер инцидента", incidentId, actual.id()),
                    () -> equal("Статус", IncidentStatus.READY_FOR_EXECUTION, actual.status()),
                    () -> equal("Вид", alien.getName(), actual.alienName()),
                    () -> equal("Тип события сохранён", "Подозрение на контакт", actual.eventType()),
                    () -> equal("Время обнаружения сохранено", prepared.input().detectedAt(), actual.detectedAt()),
                    () -> equal("Место сохранено", prepared.input().location(), actual.location()),
                    () -> equal("Описание сохранено", prepared.input().description(), actual.description()),
                    () -> equal("Вложение сохранено", fixtures.imageName(), actual.attachmentName()),
                    () -> equal("PNG загружен и декодирован браузером", 32, actual.attachmentPreviewWidth())
            );
        });
        List<HistoryEntry> history = uiStep("Открыть историю классификации инцидента", () -> card.readHistory(3));
        Allure.step("Проверить историю классификации и отправки на выполнение", () -> {
            snapshot("История классификации", history);
            assertAll(
                    () -> equal("Количество записей истории", 3, history.size()),
                    () -> equal("Выбранный вид в записи классификации",
                            "— → " + alien.getName() + " (#" + alien.getId() + ")",
                            history.get(1).changes().get("Тип инопланетянина")),
                    () -> equal("Автор записи классификации", analyst.login(), history.get(1).author()),
                    () -> equal("Переход статуса в истории", "Готов к анализу → Готов к выполнению",
                            history.get(2).changes().get("Статус")),
                    () -> equal("Автор отправки на выполнение", analyst.login(), history.get(2).author())
            );
        });
    }
}
