package gov.mib.aims.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** Список инцидентов и открытие формы регистрации. */
public class IncidentsPage {
    private final WebDriver driver;
    private final String baseUrl;
    private final WebDriverWait wait;

    /** Создаёт Page Object списка. */
    public IncidentsPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    /** Открывает форму создания инцидента. */
    public IncidentCreateForm openCreateForm() {
        wait.until(ExpectedConditions.elementToBeClickable(By.id("incident-create"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("incident-form")));
        return new IncidentCreateForm(driver, baseUrl);
    }

    /** Находит строку по точному значению видимого места происшествия. */
    public IncidentDetailsPage openByLocation(String location) {
        wait.until(browser -> {
            List<WebElement> rows = browser.findElements(By.cssSelector("#incidents-page tbody tr"));
            Optional<WebElement> match = rows.stream().filter(row -> row.findElements(By.tagName("td")).stream()
                    .anyMatch(cell -> cell.getText().equals(location))).findFirst();
            if (match.isEmpty()) {
                return false;
            }
            match.get().click();
            return true;
        });
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("incident-location")));
        return new IncidentDetailsPage(driver, baseUrl);
    }
}
