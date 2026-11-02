package gov.mib.aims.backend.services.status;

/**
 * Предусловие перехода статуса.
 *
 * @param <T> тип контекста
 */
public interface StatusTransitionPrecondition<T> {

    /**
     * Проверяет предусловие.
     *
     * @param context контекст перехода
     */
    void check(T context);
}
