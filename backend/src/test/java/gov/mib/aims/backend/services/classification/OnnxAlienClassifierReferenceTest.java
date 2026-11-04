package gov.mib.aims.backend.services.classification;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.mib.aims.backend.config.AlienClassifierProperties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверка встроенной модели по семи эталонам Python.
 */
class OnnxAlienClassifierReferenceTest {

    @ParameterizedTest
    @ValueSource(strings = {"source-01.png", "source-02.png", "source-03.png", "portrait.png",
            "grayscale.png", "rgba.png", "oriented.jpg"})
    void matchesPythonPredictionAndScore(String image) throws Exception {
        var properties = new AlienClassifierProperties();
        byte[] bytes = getClass().getResourceAsStream("/alien-classifier/" + image).readAllBytes();
        String name = image.substring(0, image.lastIndexOf('.'));
        byte[] expected;
        try (var stream = getClass().getResourceAsStream("/alien-classifier/" + name + ".logits.f32")) {
            expected = stream.readAllBytes();
        }
        var values = ByteBuffer.wrap(expected).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        float[] logits = new float[values.remaining()];
        values.get(logits);
        int best = 0;
        for (int index = 1; index < logits.length; index++) {
            if (logits[index] > logits[best]) {
                best = index;
            }
        }
        double sum = 0;
        for (float value : logits) {
            sum += Math.exp((double) value - logits[best]);
        }
        AlienClassifierModel.Label[] labels;
        try (var stream = properties.getModelPath().createRelative("labels.json").getInputStream()) {
            labels = new ObjectMapper().readValue(stream, AlienClassifierModel.Label[].class);
        }
        try (var classifier = new OnnxAlienClassifier(properties, new ObjectMapper())) {
            classifier.start();
            var prediction = classifier.predict(() -> bytes);
            assertThat(prediction.classificationKey()).isEqualTo(labels[best].classificationKey());
            assertThat(prediction.score()).isCloseTo(1 / sum, org.assertj.core.data.Offset.offset(0.0001));
        }
    }
}
