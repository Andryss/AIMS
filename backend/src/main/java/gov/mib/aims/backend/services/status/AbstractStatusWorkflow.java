package gov.mib.aims.backend.services.status;

/**
 * Общая последовательность проверки и выполнения перехода статуса.
 *
 * @param <T> тип контекста
 * @param <S> тип статуса
 */
public abstract class AbstractStatusWorkflow<T, S> {

    /**
     * Проверяет предусловия, применяет статус и выполняет действия после сохранения.
     */
    protected final T applyTransition(T context, StatusTransition<T, S> transition) {
        for (StatusTransitionPrecondition<T> precondition : transition.getPreconditions()) {
            precondition.check(context);
        }

        T updated = applyStatus(context, transition.getTo());

        for (StatusTransitionPostAction<T> postAction : transition.getPostActions()) {
            postAction.execute(updated);
        }

        return updated;
    }

    /**
     * Применяет новый статус и сохраняет изменения соответствующей сущности.
     */
    protected abstract T applyStatus(T context, S target);
}
