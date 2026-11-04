package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.AlienEntity;
import gov.mib.aims.backend.generated.model.AlienListResponse;
import gov.mib.aims.backend.generated.model.AlienResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Преобразование данных в представления API.
 */
@Component
public class AlienMapper {

    /**
     * Создаёт представление, независимое от исходной сущности.
     */
    public AlienResponse toResponse(AlienEntity entity) {
        return new AlienResponse()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .threatLevel(entity.getThreatLevel().intValue())
                .photoFileIds(new ArrayList<>(entity.getPhotoFileIds()));
    }

    /**
     * Собирает элементы и метаданные страницы для API.
     */
    public AlienListResponse toListResponse(Page<AlienEntity> page) {
        return new AlienListResponse().items(page.map(this::toResponse).getContent())
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages());
    }
}
