package gov.mib.aims.backend.services;

import gov.mib.aims.backend.model.AlienPrediction;

import java.util.List;
import java.util.function.Supplier;

/**
 * Локальное распознавание вида без изменения данных приложения.
 */
public interface AlienClassifier {

    /**
     * Возвращает лучшего кандидата, оценку и результат проверки порога модели.
     */
    AlienPrediction predict(List<Supplier<byte[]>> images);

    /**
     * Распознаёт единственное изображение тем же способом агрегации.
     */
    default AlienPrediction predict(Supplier<byte[]> image) {
        return predict(List.of(image));
    }
}
