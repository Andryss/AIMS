package gov.mib.aims.backend.services.classification;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

/**
 * Взвешивание полных распределений вместо голосования только за лучшие классы.
 */
class AlienClassificationScoresTest {

    @ParameterizedTest
    @CsvSource({"0.9,0.2,0.5705882353", "0.9,0.7,0.8125", "0.5,0.5,0.5"})
    void weightsEachDistributionByItsBestConfidence(double first, double second, double expected) {
        var scores = new AlienClassificationScores(2);
        scores.add(new float[]{(float) Math.log(first), (float) Math.log(1 - first)});
        scores.add(new float[]{(float) Math.log(second), (float) Math.log(1 - second)});
        var result = scores.prediction(List.of("first", "second"), 0.6);
        assertThat(result.classificationKey()).isEqualTo("first");
        assertThat(result.score()).isCloseTo(expected, offset(0.000001));
        assertThat(result.matched()).isEqualTo(expected >= 0.6);
    }
}
