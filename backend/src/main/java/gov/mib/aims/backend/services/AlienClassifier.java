package gov.mib.aims.backend.services;

import gov.mib.aims.backend.model.AlienPrediction;

import java.util.function.Supplier;

/**
 * Локальное распознавание вида без изменения данных приложения.
 */
public interface AlienClassifier {

    /**
     * Возвращает лучшего кандидата, оценку и результат проверки порога модели.
     */
    AlienPrediction predict(Supplier<byte[]> image);
}
