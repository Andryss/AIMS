package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.AlienEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.AlienListResponse;
import gov.mib.aims.backend.generated.model.AlienResponse;
import gov.mib.aims.backend.generated.model.AlienSearchResponse;
import gov.mib.aims.backend.generated.model.SaveAlienRequest;
import gov.mib.aims.backend.repository.AlienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Реализация {@link AlienService}.
 */
@Service
@RequiredArgsConstructor
public class AlienServiceImpl implements AlienService {

    private static final int SEARCH_LIMIT = 20;

    private final AlienRepository alienRepository;

    @Override
    @Transactional(readOnly = true)
    public AlienListResponse list(String q, int page, int size) {
        Page<AlienEntity> result = findAliens(q, page, size);
        return new AlienListResponse()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages());
    }

    @Override
    @Transactional
    public AlienResponse create(SaveAlienRequest request) {
        AlienEntity entity = new AlienEntity();
        applyChanges(entity, request);
        return toResponse(alienRepository.save(entity));
    }

    @Override
    @Transactional
    public AlienResponse update(Long id, SaveAlienRequest request) {
        AlienEntity entity = alienRepository.findById(id).orElseThrow(Errors::alienNotFound);
        applyChanges(entity, request);
        return toResponse(alienRepository.save(entity));
    }

    private void applyChanges(AlienEntity entity, SaveAlienRequest request) {
        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription().trim());
        entity.setThreatLevel(request.getThreatLevel().shortValue());
    }

    @Override
    @Transactional(readOnly = true)
    public AlienSearchResponse search(String query) {
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            throw Errors.validationError("Search query must not be empty");
        }
        return new AlienSearchResponse()
                .items(findAliens(trimmed, 0, SEARCH_LIMIT).stream()
                        .map(this::toResponse)
                        .toList());
    }

    private Page<AlienEntity> findAliens(String q, int page, int size) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return alienRepository.search(query, PageRequest.of(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public AlienResponse getById(Long id) {
        AlienEntity entity = alienRepository.findById(id)
                .orElseThrow(Errors::alienNotFound);
        return toResponse(entity);
    }

    private AlienResponse toResponse(AlienEntity entity) {
        return new AlienResponse()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .threatLevel(entity.getThreatLevel().intValue());
    }
}
