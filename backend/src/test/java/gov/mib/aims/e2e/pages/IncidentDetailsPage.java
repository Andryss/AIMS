package gov.mib.aims.e2e.pages;

import gov.mib.aims.backend.model.IncidentStatus;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Чтение карточки, смена статуса, выбор вида и просмотр истории через UI. */
public class IncidentDetailsPage {
    private final WebDriver driver;
    private final String baseUrl;
    private final WebDriverWait wait;

    /** Снимок видимых полей карточки. */
    public record State(long id, IncidentStatus status, String eventType, String location,
                        LocalDateTime detectedAt, String description, String attachmentName,
                        int attachmentPreviewWidth, String alienName) { }

    /** Одна запись истории с автором и фактическими значениями изменённых полей. */
    public record HistoryEntry(String author, String heading, Map<String, String> changes) { }

    /** Создаёт Page Object карточки инцидента. */
    public IncidentDetailsPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    /** Открывает карточку по номеру и ждёт загрузки полей. */
    public void open(long id) {
        driver.get(baseUrl + "/incidents/" + id);
        waitUntilLoaded();
    }

    /** Возвращается к списку по ссылке из карточки. */
    public IncidentsPage backToList() {
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("← К списку"))).click();
        return new IncidentsPage(driver, baseUrl);
    }

    /** Ждёт загрузки полей и превью вложений перед дальнейшими действиями. */
    void waitUntilLoaded() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("incident-location")));
        wait.until(browser -> browser.findElements(By.cssSelector(
                "#incident-attachments .attachment-card__name")).stream()
                .noneMatch(file -> file.getText().startsWith("Вложение №")));
        wait.until(browser -> browser.findElements(By.cssSelector("#incident-attachments img")).stream()
                .allMatch(image -> Integer.parseInt(image.getDomProperty("naturalWidth")) > 0));
    }

    /** Читает номер из заголовка карточки. */
    public long readId() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("incident-location")));
        return Long.parseLong(wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("incident-title"))).getText().replace("Инцидент #", ""));
    }

    /** Читает серверный статус из контролируемого select. */
    public IncidentStatus readStatus() {
        WebElement select = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("#incident-status select")));
        return IncidentStatus.valueOf(new Select(select).getFirstSelectedOption().getAttribute("value"));
    }

    /** Выбирает статус и ждёт завершения серверной операции. */
    public void changeStatus(IncidentStatus status) {
        By by = By.cssSelector("#incident-status select");
        new Select(wait.until(ExpectedConditions.elementToBeClickable(by))).selectByValue(status.name());
        wait.until(browser -> readStatus() == status
                && !browser.findElement(by).getAttribute("class").contains("is-loading"));
    }

    /** Открывает ручной выбор вида. */
    public AlienPickerDrawer openAlienPicker() {
        wait.until(ExpectedConditions.elementToBeClickable(By.id("incident-alien-select"))).click();
        return new AlienPickerDrawer(driver);
    }

    /** Читает имя привязанного вида. */
    public String readAlienName() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("#incident-alien strong"))).getText();
    }

    /** Считывает загруженные поля и имя вложения, нормализуя локальное время. */
    public State readState() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("#incident-attachments .attachment-card__name")));
        wait.until(browser -> !browser.findElement(By.cssSelector(
                "#incident-attachments .attachment-card__name")).getText().startsWith("Вложение №"));
        String fileName = driver.findElement(By.cssSelector("#incident-attachments .attachment-card__name")).getText();
        WebElement preview = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("#incident-attachments img")));
        wait.until(browser -> Integer.parseInt(preview.getDomProperty("naturalWidth")) > 0);
        int previewWidth = Integer.parseInt(preview.getDomProperty("naturalWidth"));
        String date = text("incident-detected-at").replace('\u00a0', ' ');
        List<WebElement> alien = driver.findElements(By.cssSelector("#incident-alien strong"));
        return new State(readId(), readStatus(), text("incident-event-type"), text("incident-location"),
                LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss")),
                text("incident-description"), fileName, previewWidth, alien.isEmpty() ? null : alien.get(0).getText());
    }

    /** Открывает историю и ждёт появления ожидаемого количества записей. */
    public List<HistoryEntry> readHistory(int expectedEntries) {
        driver.findElement(By.id("incident-tab-history")).click();
        wait.until(browser -> browser.findElements(By.cssSelector(
                "#incident-tab-panel-history .history-timeline__item")).size() >= expectedEntries);
        // До завершения отдельных запросов имена в истории представлены значениями #id.
        wait.until(browser -> browser.findElements(By.cssSelector(
                "#incident-tab-panel-history .user-chip__login")).stream()
                .noneMatch(user -> user.getText().startsWith("#")));
        wait.until(browser -> browser.findElements(By.cssSelector(
                "#incident-tab-panel-history .history-diff__row")).stream()
                .noneMatch(row -> row.getText().matches("(?s)Тип инопланетянина:.*(?:→\\s*)?#[0-9]+\\s*$")));
        return driver.findElements(By.cssSelector("#incident-tab-panel-history .history-timeline__item"))
                .stream().map(item -> {
                    Map<String, String> changes = new LinkedHashMap<>();
                    for (WebElement row : item.findElements(By.cssSelector(".history-diff__row"))) {
                        String label = row.findElement(By.cssSelector(".history-diff__label"))
                                .getText().replace(":", "");
                        String value = row.findElement(By.cssSelector(".text-pre-wrap"))
                                .getText().replaceAll("\\s+", " ");
                        changes.put(label, value);
                    }
                    return new HistoryEntry(item.findElement(By.cssSelector(".user-chip__login")).getText(),
                            item.findElement(By.cssSelector(".history-timeline__header")).getText(), changes);
                }).toList();
    }

    private String text(String id) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(id))).getText();
    }
}
