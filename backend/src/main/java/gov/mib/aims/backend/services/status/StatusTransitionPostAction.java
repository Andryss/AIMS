package gov.mib.aims.backend.services.status;

/**
 * Пост-действие после перехода статуса.
 *
 * @param <T> тип контекста
 */
public interface StatusTransitionPostAction<T> {

    /**
     * Выполняет пост-действие.
     *
     * @param context контекст перехода
     */
    void execute(T context);
}
