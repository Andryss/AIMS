package gov.mib.aims.e2e;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Map;

/** Поддерживаемые браузеры и их настройки для E2E-сценариев. */
public enum E2EBrowser {
    CHROME("Chrome") {
        @Override
        public WebDriver createDriver() {
            ChromeOptions options = new ChromeOptions();
            options.setBrowserVersion("156.0.8078.80");
            options.addArguments("--headless=new", "--window-size=1440,1000", "--lang=ru-RU");
            // Предупреждение Chrome о готовых тестовых паролях блокирует ввод в headless-режиме.
            options.setExperimentalOption("prefs", Map.of("profile.password_manager_leak_detection", false));
            return new ChromeDriver(options);
        }
    };

    private final String displayName;

    E2EBrowser(String displayName) {
        this.displayName = displayName;
    }

    /** Создаёт отдельную браузерную сессию для одного запуска сценария. */
    public abstract WebDriver createDriver();

    @Override
    public String toString() {
        return displayName;
    }
}
