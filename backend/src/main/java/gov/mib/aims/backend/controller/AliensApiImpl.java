package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.api.AliensApi;
import gov.mib.aims.backend.generated.model.AlienResponse;
import gov.mib.aims.backend.generated.model.AlienSearchResponse;
import gov.mib.aims.backend.services.AlienService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер справочника инопланетян.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class AliensApiImpl implements AliensApi {

    private final AlienService alienService;

    @Override
    @PreAuthorize("hasAuthority('ALIEN_READ')")
    public AlienSearchResponse searchAliens(String q) {
        log.info("GET /api/v1/aliens/search q={}", q);
        return alienService.search(q);
    }

    @Override
    @PreAuthorize("hasAuthority('ALIEN_READ')")
    public AlienResponse getAlien(Long id) {
        log.info("GET /api/v1/aliens/{}", id);
        return alienService.getById(id);
    }
}
