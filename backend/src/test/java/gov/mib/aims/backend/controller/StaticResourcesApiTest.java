package gov.mib.aims.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StaticResourcesApiTest extends BaseApiTest {

    @Test
    void publicIndex_isAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/public/index.html"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/html"));
    }

    @Test
    void publicImage_isAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/public/mib_logo.png"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));
    }

    @Test
    void publicBundlesFromAssetManifest_areAccessibleWithoutAuth() throws Exception {
        String manifestJson = new String(
                new ClassPathResource("static/asset-manifest.json").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
        JsonNode manifest = objectMapper.readTree(manifestJson);
        String mainJs = manifest.path("files").path("main.js").asText();
        String mainCss = manifest.path("files").path("main.css").asText();

        mockMvc.perform(get(mainJs))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/javascript"));

        mockMvc.perform(get(mainCss))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/css"));
    }
}
