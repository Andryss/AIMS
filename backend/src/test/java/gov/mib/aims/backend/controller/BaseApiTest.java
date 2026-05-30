package gov.mib.aims.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.mib.aims.backend.BaseDbTest;
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
    protected ObjectMapper objectMapper;

    protected static final MediaType APPLICATION_JSON = MediaType.APPLICATION_JSON;
}
