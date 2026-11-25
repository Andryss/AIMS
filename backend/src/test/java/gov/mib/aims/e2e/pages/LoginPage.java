package gov.mib.aims.e2e.pages;

import gov.mib.aims.e2e.E2EFixtures.Account;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** Форма входа, работающая с настоящей браузерной сессией. */
public class LoginPage {
    private final WebDriver driver;
    private final String baseUrl;

    /** Создаёт Page Object для адреса пользовательского интерфейса. */
    public LoginPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
    }

    /** Входит через форму и ожидает появления списка инцидентов. */
    public IncidentsPage signIn(Account account) {
        driver.get(baseUrl + "/incidents");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.elementToBeClickable(By.id("login-username"))).sendKeys(account.login());
        driver.findElement(By.id("login-password")).sendKeys(account.password());
        driver.findElement(By.id("login-submit")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("incidents-page")));
        return new IncidentsPage(driver, baseUrl);
    }
}
