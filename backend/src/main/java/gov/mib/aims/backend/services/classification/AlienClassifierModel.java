package gov.mib.aims.backend.services.classification;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Проверенный комплект модели: веса, порядок классов и правило принятия результата.
 */
record AlienClassifierModel(byte[] weights, List<String> keys, double threshold, String input, String output) {

    private static final int CLASS_COUNT = 50;
    private static final int FORMAT_VERSION = 1;
    private static final int MAX_MODEL_BYTES = 64 * 1024 * 1024;
    private static final int MAX_JSON_BYTES = 1024 * 1024;

    static AlienClassifierModel load(Resource model, ObjectMapper mapper) throws IOException {
        Manifest manifest = mapper.readValue(read(model.createRelative("model-manifest.json"), MAX_JSON_BYTES),
                Manifest.class);
        require(manifest != null && manifest.formatVersion() == FORMAT_VERSION && manifest.classCount() == CLASS_COUNT
                && manifest.input() != null && manifest.output() != null && manifest.decision() != null
                && manifest.files() != null);
        byte[] weights = read(model, MAX_MODEL_BYTES);
        byte[] labelsBytes = read(model.createRelative("labels.json"), MAX_JSON_BYTES);
        byte[] preprocessing = read(model.createRelative("preprocessing.json"), MAX_JSON_BYTES);
        for (var file : Map.of("model.onnx", weights, "labels.json", labelsBytes,
                "preprocessing.json", preprocessing).entrySet()) {
            require(hash(file.getValue()).equals(manifest.files().get(file.getKey())));
        }
        Label[] labels = mapper.readerFor(Label[].class)
                .without(DeserializationFeature.ACCEPT_FLOAT_AS_INT).readValue(labelsBytes);
        require(labels != null && labels.length == CLASS_COUNT);
        for (int index = 0; index < labels.length; index++) {
            Label label = labels[index];
            require(label != null && label.index() != null && label.index() == index
                    && label.classificationKey() != null && !label.classificationKey().isBlank());
        }
        List<String> keys = java.util.Arrays.stream(labels).map(Label::classificationKey).toList();
        require(new HashSet<>(keys).size() == CLASS_COUNT);
        Decision decision = manifest.decision();
        Double threshold = decision.threshold();
        require("softmax-max".equals(decision.type()) && threshold != null
                && Double.isFinite(threshold) && threshold >= 0 && threshold <= 1);
        String input = manifest.input().name();
        String output = manifest.output().name();
        require(input != null && !input.isBlank() && output != null && !output.isBlank());
        return new AlienClassifierModel(weights, keys, threshold, input, output);
    }

    /** Метка выхода модели и внутренний ключ справочника. */
    record Label(Integer index, String classificationKey) {
    }

    /** Метаданные, необходимые для загрузки и проверки модели. */
    private record Manifest(int formatVersion, int classCount, Tensor input, Tensor output,
                            Decision decision, Map<String, String> files) {
    }

    /** Имя входного или выходного тензора. */
    private record Tensor(String name) {
    }

    /** Правило принятия результата модели. */
    private record Decision(String type, Double threshold) {
    }

    private static byte[] read(Resource resource, int limit) throws IOException {
        try (var stream = resource.getInputStream()) {
            byte[] bytes = stream.readNBytes(limit + 1);
            require(bytes.length <= limit);
            return bytes;
        }
    }

    private static String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void require(boolean condition) throws IOException {
        if (!condition) {
            throw new IOException("Invalid alien classifier model bundle");
        }
    }
}
