package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.generated.model.UpdateIncidentRequest;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.incident.IncidentDetails;
import gov.mib.aims.backend.services.validation.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Единая подготовка основных сведений из запросов создания и изменения.
 */
@Component
@RequiredArgsConstructor
public class IncidentInputMapper {

    private final IncidentMapper incidentMapper;
    private final ChronoService chronoService;

    /**
     * Подготавливает сведения нового инцидента.
     */
    public IncidentDetails from(CreateIncidentRequest request) {
        return normalize(request.getEventType(), request.getLocation(), request.getDetectedAt(),
                request.getDescription(), request.getVictimCount());
    }

    /**
     * Подготавливает изменяемые сведения инцидента.
     */
    public IncidentDetails from(UpdateIncidentRequest request) {
        return normalize(request.getEventType(), request.getLocation(), request.getDetectedAt(),
                request.getDescription(), request.getVictimCount());
    }

    private IncidentDetails normalize(IncidentEventTypeApi eventType, String location, OffsetDateTime detectedAt,
                                      String description, Integer victimCount) {
        String message = "Укажите место и описание инцидента";

        return new IncidentDetails(incidentMapper.toModelEventType(eventType),
                TextNormalizer.required(location, message),
                chronoService.truncate(chronoService.toLocalDateTime(detectedAt), ChronoUnit.MICROS),
                TextNormalizer.required(description, message), victimCount);
    }
}
