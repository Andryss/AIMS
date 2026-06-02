package gov.mib.aims.backend.model;

import gov.mib.aims.backend.exception.Errors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ссылка на сущность в формате {@code ENTITY_TYPE:id}.
 */
public record EntityRef(EntityType entityType, long entityId) {

    private static final Pattern REF_PATTERN = Pattern.compile("^([A-Z_]+):(\\d+)$");

    /**
     * Формирует строковую ссылку.
     *
     * @param entityType тип сущности
     * @param entityId идентификатор сущности
     * @return ссылка вида INCIDENT:42
     */
    public static String format(EntityType entityType, long entityId) {
        return entityType.name() + ":" + entityId;
    }

    /**
     * Разбирает и валидирует строковую ссылку.
     *
     * @param ref ссылка
     * @return разобранная ссылка
     */
    public static EntityRef parse(String ref) {
        if (ref == null || ref.isBlank()) {
            throw Errors.invalidRelatedEntity(ref);
        }
        Matcher matcher = REF_PATTERN.matcher(ref.trim());
        if (!matcher.matches()) {
            throw Errors.invalidRelatedEntity(ref);
        }
        String typeName = matcher.group(1);
        long id = Long.parseLong(matcher.group(2));
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            throw Errors.invalidRelatedEntity(ref);
        }
        return new EntityRef(entityType, id);
    }
}
