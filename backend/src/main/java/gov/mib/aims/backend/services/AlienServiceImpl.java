package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.AlienEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.AlienResponse;
import gov.mib.aims.backend.generated.model.AlienSearchResponse;
import gov.mib.aims.backend.repository.AlienRepository;
import lombok.RequiredArgsConstructor;
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
    public AlienSearchResponse search(String query) {
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            throw Errors.validationError("Search query must not be empty");
        }
        String pattern = "%" + trimmed.toLowerCase(Locale.ROOT) + "%";
        return new AlienSearchResponse()
                .items(alienRepository.search(pattern, SEARCH_LIMIT).stream()
                        .map(this::toResponse)
                        .toList());
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
