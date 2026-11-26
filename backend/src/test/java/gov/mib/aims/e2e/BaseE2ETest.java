package gov.mib.aims.e2e;

import gov.mib.aims.backend.BackendApplication;
import gov.mib.aims.backend.config.StorageProperties;
import gov.mib.aims.backend.support.AllureTestDataWriter;
import gov.mib.aims.backend.support.AllureRunMetadataExtension;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.jupiter.AllureJupiter;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Реальный HTTP-стенд и новая браузерная сессия для каждого сценария. */
@SpringBootTest(classes = BackendApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase(
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD,
        type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES,
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
)
@Import(E2EFixtures.class)
@ExtendWith({AllureJupiter.class, AllureRunMetadataExtension.class})
@Tag("e2e")
@Epic("Бизнес-цикл AIMS")
public abstract class BaseE2ETest {

    private static final List<E2EBrowser> BROWSERS = List.of(E2EBrowser.CHROME);

    @LocalServerPort
    private int port;
    @Autowired
    private StorageProperties storageProperties;
    @Autowired
    protected E2EFixtures fixtures;
    @TempDir
    private Path storage;
    protected WebDriver driver;
    protected ZoneId browserZone;

    @BeforeAll
    static void recordDatabaseVersionForAllure(@Autowired DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            System.setProperty("aims.test.environment.PostgreSQL",
                    connection.getMetaData().getDatabaseProductVersion());
        }
    }

    @BeforeEach
    void prepareTestEnvironment() {
        storageProperties.setBasePath(storage.toString());
    }

    protected static Stream<E2EBrowser> browsers() {
        return BROWSERS.stream();
    }

    protected void startBrowser(E2EBrowser browser) {
        Allure.step("Подготовить браузер " + browser, () -> configureBrowser(browser));
    }

    private void configureBrowser(E2EBrowser browser) {
        driver = browser.createDriver();
        driver.manage().window().setSize(new Dimension(1440, 1000));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        String browserVersion = ((HasCapabilities) driver).getCapabilities().getBrowserVersion();
        System.setProperty("aims.test.environment.Browsers", BROWSERS.stream()
                .map(E2EBrowser::toString).collect(Collectors.joining(", ")));
        String viewport = (String) ((JavascriptExecutor) driver)
                .executeScript("return window.innerWidth + 'x' + window.innerHeight");
        System.setProperty("aims.test.environment.Viewport", viewport);
        String timezone = (String) ((JavascriptExecutor) driver)
                .executeScript("return Intl.DateTimeFormat().resolvedOptions().timeZone");
        browserZone = ZoneId.of(timezone);
        System.setProperty("aims.test.environment.Browser.timezone", timezone);
        Allure.parameter("Версия браузера", browserVersion, true);
    }

    @AfterEach
    void stopBrowser() {
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                driver = null;
            }
        }
    }

    protected String baseUrl() {
        return "http://localhost:" + port + "/public";
    }

    /** Записывает UI-действие и снимок его результата в один шаг Allure. */
    protected void uiStep(String name, Runnable action) {
        uiStep(name, () -> {
            action.run();
            return null;
        });
    }

    /** Возвращает результат UI-действия после сохранения скриншота в Allure. */
    protected <T> T uiStep(String name, Supplier<T> action) {
        return Allure.step(name, () -> {
            T result = action.get();
            attachScreenshot(name);
            return result;
        });
    }

    private void attachScreenshot(String name) {
        AllureTestDataWriter.png(name, ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
    }
}
