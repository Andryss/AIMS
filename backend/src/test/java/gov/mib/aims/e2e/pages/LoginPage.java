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
    private final WebDriverWait wait;

    /** Создаёт Page Object для адреса пользовательского интерфейса. */
    public LoginPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    /** Подготавливает авторизованную сессию перед шагами прецедента. */
    public IncidentsPage signIn(Account account) {
        open();
        fill(account);
        return signIn();
    }

    /** Открывает форму входа. */
    public void open() {
        driver.get(baseUrl + "/incidents");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-username")));
    }

    /** Заполняет учётные данные. */
    public void fill(Account account) {
        wait.until(ExpectedConditions.elementToBeClickable(By.id("login-username"))).sendKeys(account.login());
        driver.findElement(By.id("login-password")).sendKeys(account.password());
    }

    /** Отправляет форму и ожидает загрузки списка инцидентов. */
    public IncidentsPage signIn() {
        driver.findElement(By.id("login-submit")).click();
        return new IncidentsPage(driver, baseUrl);
    }
}
