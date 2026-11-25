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

        // Подготовка исходной страницы: вход аналитиком и открытие инцидента.
        Allure.step("Войти аналитиком", () -> new LoginPage(driver, baseUrl()).signIn(analyst));
        IncidentDetailsPage card = new IncidentDetailsPage(driver, baseUrl());
        card.open(incidentId);

        // Проверка исходного состояния.
        equal("Исходный статус", IncidentStatus.READY_FOR_ANALYSIS, card::readStatus);

        // Действие: ручная классификация инцидента.
        Allure.step("Найти и выбрать вид в справочнике", () -> {
            AlienPickerDrawer picker = card.openAlienPicker();
            picker.select(alien.getName());

            // Проверка результата выбора вида.
            equal("Выбранный вид", alien.getName(), card::readAlienName);
            attachScreenshot("Результат ручной классификации");
        });

        // Действие: отправка инцидента на выполнение.
        Allure.step("Отправить инцидент на выполнение", () ->
                card.changeStatus(IncidentStatus.READY_FOR_EXECUTION));

        // Проверки сохранённых данных и истории изменений.
        Allure.step("Повторно открыть инцидент и проверить результат", () -> {
            card.reopen(incidentId);
            State actual = card.readState();
            snapshot("Карточка после классификации", actual);
            attachScreenshot("Сохранённая классификация");
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
        Allure.step("Проверить историю классификации и отправки на выполнение", () -> {
            List<HistoryEntry> history = card.readHistory(3);
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
            attachScreenshot("История классификации");
        });
    }
}
