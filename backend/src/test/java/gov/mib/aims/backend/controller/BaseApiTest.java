package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.BaseDbTest;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.services.ObjectMapperWrapper;
import gov.mib.aims.backend.support.ApiTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Базовый класс для API-тестов с MockMvc.
 */
@AutoConfigureMockMvc
public abstract class BaseApiTest extends BaseDbTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapperWrapper objectMapper;

    @Autowired
    protected AppUserRepository appUserRepository;

    protected ApiTestFixtures fixtures;

    protected static final MediaType APPLICATION_JSON = MediaType.APPLICATION_JSON;

    @BeforeEach
    void initApiTestFixtures() {
        fixtures = new ApiTestFixtures(mockMvc, objectMapper, appUserRepository);
    }
}
