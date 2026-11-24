package gov.mib.aims.backend.support;

import io.qameta.allure.Epic;
import io.qameta.allure.jupiter.AllureJupiter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Общая настройка Mockito, Allure и метаданных функционального прогона.
 */
@ExtendWith({AllureJupiter.class, MockitoExtension.class, FunctionalTestRunExtension.class})
@Epic("Функциональное тестирование")
public abstract class BaseFunctionalTest {
}
