package gov.mib.aims.backend.services.alien;

import gov.mib.aims.backend.entity.AlienEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.AlienListResponse;
import gov.mib.aims.backend.generated.model.AlienResponse;
import gov.mib.aims.backend.generated.model.AlienSearchResponse;
import gov.mib.aims.backend.generated.model.SaveAlienRequest;
import gov.mib.aims.backend.repository.AlienRepository;
import gov.mib.aims.backend.services.mapping.AlienMapper;
import gov.mib.aims.backend.services.query.PageParameters;
import gov.mib.aims.backend.services.validation.AttachmentValidator;
import gov.mib.aims.backend.services.validation.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Реализация {@link AlienService}.
 */
@Service
@RequiredArgsConstructor
public class AlienServiceImpl implements AlienService {

    private final AlienMapper alienMapper;

    private static final int SEARCH_LIMIT = 20;
    private static final int MAX_PHOTOS = 20;

    private final AlienRepository alienRepository;
    private final AttachmentValidator attachmentValidator;

    @Override
    @Transactional(readOnly = true)
    public AlienListResponse list(String q, int page, int size) {
        Page<AlienEntity> result = findAliens(q, page, size);
        return alienMapper.toListResponse(result);
    }

    @Override
    @Transactional
    public AlienResponse create(SaveAlienRequest request) {
        AlienEntity entity = new AlienEntity();
        applyChanges(entity, request);
        return alienMapper.toResponse(alienRepository.save(entity));
    }

    @Override
    @Transactional
    public AlienResponse update(Long id, SaveAlienRequest request) {
        AlienEntity entity = alienRepository.findById(id).orElseThrow(Errors::alienNotFound);
        applyChanges(entity, request);
        return alienMapper.toResponse(alienRepository.save(entity));
    }

    private void applyChanges(AlienEntity entity, SaveAlienRequest request) {
        entity.setName(TextNormalizer.required(request.getName(), "Укажите имя инопланетянина"));
        entity.setDescription(TextNormalizer.required(request.getDescription(), "Укажите описание инопланетянина"));
        entity.setThreatLevel(request.getThreatLevel().shortValue());

        if (request.getPhotoFileIds() != null) {
            validatePhotos(request.getPhotoFileIds());
            entity.setPhotoFileIds(request.getPhotoFileIds());
        }
    }

    private void validatePhotos(List<Long> fileIds) {
        attachmentValidator.assertDistinctWithinLimit(fileIds, MAX_PHOTOS, "Укажите до 20 разных фотографий");
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
                        .map(alienMapper::toResponse)
                        .toList());
    }

    private Page<AlienEntity> findAliens(String q, int page, int size) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return alienRepository.search(query, PageParameters.of(page, size).toPageable());
    }

    @Override
    @Transactional(readOnly = true)
    public AlienResponse getById(Long id) {
        AlienEntity entity = alienRepository.findById(id)
                .orElseThrow(Errors::alienNotFound);
        return alienMapper.toResponse(entity);
    }

}
