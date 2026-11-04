package gov.mib.aims.backend.services.status;

/**
 * Общий исполнитель предусловий и действий для переходов, зарегистрированных в графе.
 *
 * @param <T> тип контекста
 * @param <S> тип статуса
 */
public abstract class AbstractGraphStatusWorkflow<T, S> {

    /**
     * Проверяет предусловия и изменяет статус без сохранения и пост-действий.
     */
    protected final T applyTransition(T context, StatusTransition<T, S> transition) {
        for (StatusTransitionPrecondition<T> precondition : transition.getPreconditions()) {
            precondition.check(context);
        }

        return applyStatus(context, transition.getTo());
    }

    /**
     * Выполняет пост-действия после сохранения сущности и истории вызывающим сервисом.
     */
    protected final void executePostActions(T context, StatusTransition<T, S> transition) {
        for (StatusTransitionPostAction<T> postAction : transition.getPostActions()) {
            postAction.execute(context);
        }
    }

    /**
     * Применяет новый статус без сохранения сущности.
     */
    protected abstract T applyStatus(T context, S target);
}
