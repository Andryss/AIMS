package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.api.AliensApi;
import gov.mib.aims.backend.generated.model.AlienResponse;
import gov.mib.aims.backend.generated.model.AlienSearchResponse;
import gov.mib.aims.backend.generated.model.AlienListResponse;
import gov.mib.aims.backend.generated.model.SaveAlienRequest;
import jakarta.validation.Valid;
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
    public AlienListResponse listAliens(String q, Integer page, Integer size) {
        log.info("GET /api/v1/aliens page={} size={}", page, size);
        return alienService.list(q, page, size);
    }

    @Override
    @PreAuthorize("hasAuthority('ALIEN_WRITE')")
    public AlienResponse createAlien(@Valid SaveAlienRequest request) {
        log.info("POST /api/v1/aliens");
        return alienService.create(request);
    }

    @Override
    @PreAuthorize("hasAuthority('ALIEN_WRITE')")
    public AlienResponse updateAlien(Long id, @Valid SaveAlienRequest request) {
        log.info("PUT /api/v1/aliens/{}", id);
        return alienService.update(id, request);
    }

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
