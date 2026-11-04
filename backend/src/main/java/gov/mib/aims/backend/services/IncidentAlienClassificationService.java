package gov.mib.aims.backend.services;

import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.IncidentAlienClassificationResponse;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.repository.AlienRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Предлагает вид по вложению, не изменяя инцидент или историю.
 */
@Service
@RequiredArgsConstructor
public class IncidentAlienClassificationService {

    private static final int CONFIDENCE_PERCENT_SCALE = 100;

    private final IncidentRepository incidentRepository;
    private final AlienRepository alienRepository;
    private final AlienService alienService;
    private final FileService fileService;
    private final AlienClassifier classifier;

    /**
     * Проверяет инцидент и вложение, затем выполняет распознавание вне транзакции БД.
     */
    public IncidentAlienClassificationResponse classify(Long incidentId, Long fileId) {
        var incident = incidentRepository.findById(incidentId).orElseThrow(Errors::incidentNotFound);
        if (incident.getStatus() != IncidentStatus.READY_FOR_ANALYSIS) {
            throw Errors.invalidAlienLink();
        }
        if (!incident.getAttachmentFileIds().contains(fileId)) {
            throw Errors.attachmentNotFound();
        }

        var prediction = classifier.predict(() -> fileService.download(fileId).content());
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
}
