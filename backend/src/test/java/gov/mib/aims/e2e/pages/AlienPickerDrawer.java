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
        wait.until(ExpectedConditions.elementToBeClickable(By.id("alien-picker-search")));
    }

    /** Вводит имя вида и ожидает его появления в результатах поиска. */
    public void search(String name) {
        WebElement search = wait.until(ExpectedConditions.elementToBeClickable(By.id("alien-picker-search")));
        search.clear();
        search.sendKeys(name);
        wait.until(browser -> findAlien(name).isPresent());
    }

    /** Выбирает найденную запись и ожидает её выделения. */
    public void select(String name) {
        WebElement card = wait.until(browser -> findAlien(name).orElse(null));
        card.click();
        wait.until(browser -> findAlien(name)
                .filter(result -> "true".equals(result.getDomAttribute("aria-pressed"))).isPresent());
    }

    /** Подтверждает выбор и ожидает закрытия дровера. */
    public void confirm() {
        wait.until(ExpectedConditions.elementToBeClickable(By.id("alien-picker-confirm"))).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("alien-picker-search")));
    }

    private Optional<WebElement> findAlien(String name) {
        return driver.findElements(By.cssSelector(".alien-picker-card")).stream()
                .filter(card -> card.findElement(By.cssSelector(".alien-card__name")).getText().equals(name))
                .findFirst();
    }
}
