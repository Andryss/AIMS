package gov.mib.aims.backend.services.incident;

/**
 * Передаёт текст комментария из смены статуса в post-action в рамках одного потока.
 */
public final class StatusChangeCommentHolder {

    private static final ThreadLocal<String> COMMENT = new ThreadLocal<>();

    private StatusChangeCommentHolder() {
    }

    /**
     * Сохраняет комментарий для текущего запроса смены статуса.
     *
     * @param comment текст комментария или null
     */
    public static void set(String comment) {
        if (comment == null || comment.isBlank()) {
            COMMENT.remove();
        } else {
            COMMENT.set(comment.trim());
        }
    }

    /**
     * Возвращает excerpt комментария для уведомления (до 120 символов).
     *
     * @return excerpt или null
     */
    public static String getCommentExcerpt() {
        String comment = COMMENT.get();
        if (comment == null || comment.isBlank()) {
            return null;
        }
        if (comment.length() <= 120) {
            return comment;
        }
        return comment.substring(0, 120) + "…";
    }

    /**
     * Очищает сохранённый комментарий.
     */
    public static void clear() {
        COMMENT.remove();
    }
}
