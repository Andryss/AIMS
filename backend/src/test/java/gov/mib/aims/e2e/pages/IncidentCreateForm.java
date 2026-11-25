package gov.mib.aims.e2e.pages;

import gov.mib.aims.e2e.E2EFixtures.IncidentInput;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

/** Поля регистрации и загрузка реального изображения. */
public class IncidentCreateForm {
    private final WebDriver driver;
    private final String baseUrl;
    private final WebDriverWait wait;

    /** Создаёт Page Object формы регистрации. */
    public IncidentCreateForm(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    /** Заполняет обязательные поля, включая нативный datetime-local через клавиатуру. */
    public void fill(IncidentInput input) {
        new Select(driver.findElement(By.id("incident-event-type-input"))).selectByValue(input.eventType().name());
        driver.findElement(By.id("incident-location-input")).sendKeys(input.location());
        driver.findElement(By.id("incident-description-input")).sendKeys(input.description());
        WebElement date = driver.findElement(By.id("incident-detected-at-input"));
        date.sendKeys(input.detectedAt().format(DateTimeFormatter.ofPattern("ddMMyyyy")));
        date.sendKeys(org.openqa.selenium.Keys.TAB);
        date.sendKeys(input.detectedAt().format(DateTimeFormatter.ofPattern("HHmm")));
    }

    /** Выбирает файл по абсолютному пути и ждёт отображения имени. */
    public void upload(Path image) {
        driver.findElement(By.id("incident-attachment-input")).sendKeys(image.toAbsolutePath().toString());
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.id("incident-form"), image.getFileName().toString()));
    }

    /** Сохраняет форму и ожидает её закрытия после успешного ответа API. */
    public IncidentsPage save() {
        driver.findElement(By.id("incident-save")).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("incident-form")));
        return new IncidentsPage(driver, baseUrl);
    }
}
