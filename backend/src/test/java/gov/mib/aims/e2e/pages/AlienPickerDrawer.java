package gov.mib.aims.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Optional;

/** Ручной поиск и выбор вида в справочнике. */
public class AlienPickerDrawer {
    private final WebDriver driver;
    private final WebDriverWait wait;

    /** Создаёт Page Object панели выбора вида. */
    public AlienPickerDrawer(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    /** Находит запись по полному имени и подтверждает выбор. */
    public void select(String name) {
        WebElement search = wait.until(ExpectedConditions.elementToBeClickable(By.id("alien-picker-search")));
        search.clear();
        search.sendKeys(name);
        wait.until(browser -> {
            Optional<WebElement> match = browser.findElements(By.cssSelector(".alien-picker-card")).stream()
                    .filter(card -> card.findElement(By.cssSelector(".alien-card__name")).getText().equals(name))
                    .findFirst();
            if (match.isEmpty()) {
                return false;
            }
            match.get().click();
            return true;
        });
        wait.until(ExpectedConditions.elementToBeClickable(By.id("alien-picker-confirm"))).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("alien-picker-search")));
    }
}
