package gov.mib.aims.backend.services.incident;

import gov.mib.aims.backend.entity.IncidentEntity;

/**
 * Данные перехода инцидента и комментарий для его пост-действий.
 */
public record IncidentTransitionContext(IncidentEntity incident, String comment) {

    /**
     * Возвращает очищенный комментарий для уведомления, сокращённый до 120 символов.
     *
     * @return текст комментария или null
     */
    public String commentExcerpt() {
        if (comment == null) {
            return null;
        }
        String trimmed = comment.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return trimmed.length() <= 120 ? trimmed : trimmed.substring(0, 120) + "…";
    }
}
