package gov.mib.aims.backend.services.status;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Переход статуса с предусловиями и действиями после сохранения.
 *
 * @param <T> тип контекста
 * @param <S> тип статуса
 */
@Value
@Builder
public class StatusTransition<T, S> {

    S from;
    S to;
    List<StatusTransitionPrecondition<T>> preconditions;
    List<StatusTransitionPostAction<T>> postActions;
}
