package gov.mib.aims.backend.services;

import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.entity.StoredFileEntity;
import gov.mib.aims.backend.repository.StoredFileRepository;

import gov.mib.aims.backend.generated.model.IncidentAlienClassificationResponse;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.repository.AlienRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Предлагает вид по вложению, не изменяя инцидент или историю.
 */
@Service
@RequiredArgsConstructor
public class IncidentAlienClassificationService {

    private static final int CONFIDENCE_PERCENT_SCALE = 100;
    private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg");

    private final IncidentRepository incidentRepository;
    private final AlienRepository alienRepository;
    private final AlienService alienService;
    private final FileService fileService;
    private final StoredFileRepository storedFileRepository;
    private final AlienClassifier classifier;

    /**
     * Проверяет инцидент и вложение, затем выполняет распознавание вне транзакции БД.
     */
    public IncidentAlienClassificationResponse classify(Long incidentId) {
        var incident = incidentRepository.findById(incidentId).orElseThrow(Errors::incidentNotFound);
        if (incident.getStatus() != IncidentStatus.READY_FOR_ANALYSIS) {
            throw Errors.invalidAlienLink();
        }

        Map<Long, StoredFileEntity> files = new HashMap<>();
        storedFileRepository.findAllById(incident.getAttachmentFileIds())
                .forEach(file -> files.put(file.getId(), file));
        List<Supplier<byte[]>> images = incident.getAttachmentFileIds().stream().distinct()
                .filter(id -> files.containsKey(id) && supported(files.get(id).getContentType()))
                .map(id -> (Supplier<byte[]>) () -> fileService.download(id).content()).toList();
        if (images.isEmpty()) {
            throw Errors.validationError("В инциденте нет доступных изображений PNG или JPEG");
        }

        var prediction = classifier.predict(images);
        var response = new IncidentAlienClassificationResponse()
                .status(IncidentAlienClassificationResponse.StatusEnum.NO_MATCH)
                .confidence(prediction.score() * CONFIDENCE_PERCENT_SCALE);
        if (!prediction.matched()) {
            return response;
        }

        return alienRepository.findByClassificationKey(prediction.classificationKey())
                .map(alien -> response.status(IncidentAlienClassificationResponse.StatusEnum.MATCH)
                        .alien(alienService.getById(alien.getId())))
                .orElse(response);
    }

    private boolean supported(String contentType) {
        return contentType != null
                && IMAGE_TYPES.contains(contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT));
    }
}
