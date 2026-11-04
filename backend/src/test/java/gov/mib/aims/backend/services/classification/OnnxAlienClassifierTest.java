package gov.mib.aims.backend.services.classification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.mib.aims.backend.config.AlienClassifierProperties;
import gov.mib.aims.backend.exception.BaseException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Выполнение компактной ONNX-модели, проверка комплекта и загрузки ресурсов из JAR.
 */
class OnnxAlienClassifierTest {

    @TempDir
    private Path directory;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void loadsBundledModelWithoutConfiguration() throws Exception {
        try (var classifier = new OnnxAlienClassifier(new AlienClassifierProperties(), mapper)) {
            classifier.start();
            var result = classifier.predict(this::image);
            assertThat(result.matched()).isTrue();
            assertThat(result.classificationKey()).isEqualTo("starfield_angler");
        }
    }

    @Test
    void readsModelAndSiblingMetadataFromJarResources() throws Exception {
        var properties = bundle();
        Path jar = directory.resolve("classifier.jar");
        try (var archive = new java.util.zip.ZipOutputStream(Files.newOutputStream(jar))) {
            for (String name : new String[]{"model.onnx", "labels.json", "preprocessing.json", "model-manifest.json"}) {
                archive.putNextEntry(new java.util.zip.ZipEntry("model/" + name));
                Files.copy(directory.resolve(name), archive);
                archive.closeEntry();
            }
        }
        properties.setModelPath(new UrlResource("jar:" + jar.toUri() + "!/model/model.onnx"));
        assertThat(properties.getModelPath().isFile()).isFalse();
        try (var classifier = new OnnxAlienClassifier(properties, mapper)) {
            classifier.start();
            assertThat(classifier.predict(this::image).classificationKey()).isEqualTo("starfield_0");
        }
    }

    @Test
    void runsOnCpuAndReturnsBestCandidate() throws Exception {
        var properties = bundle();
        try (var classifier = new OnnxAlienClassifier(properties, mapper)) {
            classifier.start();
            byte[] image = getClass().getResourceAsStream("/alien-classifier/source-01.png").readAllBytes();
            var result = classifier.predict(() -> image);
            assertThat(result.matched()).isTrue();
            assertThat(result.classificationKey()).isEqualTo("starfield_0");
            assertThat(result.score()).isBetween(0.98, 0.99);
        }
    }

    @Test
    void returnsNoMatchBelowManifestThreshold() throws Exception {
        var properties = bundle();
        ObjectNode manifest = (ObjectNode) mapper.readTree(directory.resolve("model-manifest.json").toFile());
        ((ObjectNode) manifest.get("decision")).put("threshold", 0.999);
        mapper.writeValue(directory.resolve("model-manifest.json").toFile(), manifest);
        try (var classifier = new OnnxAlienClassifier(properties, mapper)) {
            classifier.start();
            assertThat(classifier.predict(() -> image()).matched()).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "hash", "labels", "threshold", "shape", "key-type", "index-type"})
    void leavesApplicationAvailableWhenModelCannotBeUsed(String variant) throws Exception {
        var properties = bundle();
        if (variant.equals("missing")) {
            Files.delete(directory.resolve("model.onnx"));
        } else if (variant.equals("hash")) {
            Files.write(directory.resolve("model.onnx"), new byte[]{1});
        } else if (variant.equals("labels") || variant.endsWith("-type")) {
            var labels = mapper.readTree(directory.resolve("labels.json").toFile());
            if (variant.equals("key-type")) {
                ((ObjectNode) labels.get(1)).putNull("classificationKey");
            } else if (variant.equals("index-type")) {
                ((ObjectNode) labels.get(1)).put("index", 1.5);
            } else {
                ((ObjectNode) labels.get(1)).put("classificationKey", "starfield_0");
            }
            mapper.writeValue(directory.resolve("labels.json").toFile(), labels);
            updateHash("labels.json");
        } else {
            ObjectNode manifest = (ObjectNode) mapper.readTree(directory.resolve("model-manifest.json").toFile());
            if (variant.equals("threshold")) {
                ((ObjectNode) manifest.get("decision")).put("threshold", -1);
            } else {
                manifest.put("classCount", 49);
            }
            mapper.writeValue(directory.resolve("model-manifest.json").toFile(), manifest);
        }
        try (var classifier = new OnnxAlienClassifier(properties, mapper)) {
            classifier.start();
            assertThatThrownBy(() -> classifier.predict(() -> new byte[]{1})).isInstanceOf(BaseException.class)
                    .extracting("code").isEqualTo(503);
        }
    }

    @Test
    void rejectsBusyRequestsBeforeReadingTheirFiles() throws Exception {
        try (var classifier = new OnnxAlienClassifier(bundle(), mapper)) {
            classifier.start();
            var prediction = classifier.predict(() -> {
                assertThatThrownBy(() -> classifier.predict(() -> {
                    throw new AssertionError("Busy request must not read the file");
                })).isInstanceOf(BaseException.class).extracting("code").isEqualTo(503);
                return image();
            });
            assertThat(prediction.matched()).isTrue();
        }
    }

    @Test
    void processesAllImagesAndSkipsUnreadableOnes() throws Exception {
        try (var classifier = new OnnxAlienClassifier(bundle(), mapper)) {
            classifier.start();
            var reads = new java.util.concurrent.atomic.AtomicInteger();
            java.util.List<java.util.function.Supplier<byte[]>> images = java.util.stream.IntStream.range(0, 3)
                    .mapToObj(index -> (java.util.function.Supplier<byte[]>) () -> {
                        reads.incrementAndGet();
                        if (index == 0) {
                            throw gov.mib.aims.backend.exception.Errors.fileNotFound();
                        }
                        return index == 1 ? new byte[0] : image();
                    }).toList();
            assertThat(classifier.predict(images).matched()).isTrue();
            assertThat(reads.get()).isEqualTo(3);
        }
    }

    @Test
    void reportsStorageReadFailureAsUnavailableAndReleasesPermit() throws Exception {
        try (var classifier = new OnnxAlienClassifier(bundle(), mapper)) {
            classifier.start();
            assertThatThrownBy(() -> classifier.predict(() -> {
                throw new IllegalStateException("Storage read failed");
            })).isInstanceOf(BaseException.class).extracting("code").isEqualTo(503);
            assertThat(classifier.predict(this::image).matched()).isTrue();
        }
    }

    @Test
    void reportsInvalidOutputBatchAsUnavailable() throws Exception {
        var properties = bundle();
        try (var model = getClass().getResourceAsStream("/alien-classifier/empty-batch.onnx")) {
            Files.copy(model, directory.resolve("model.onnx"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        updateHash("model.onnx");
        try (var classifier = new OnnxAlienClassifier(properties, mapper)) {
            classifier.start();
            assertThatThrownBy(() -> classifier.predict(() -> new byte[0])).isInstanceOf(BaseException.class)
                    .extracting("code").isEqualTo(400);
            assertThatThrownBy(() -> classifier.predict(this::image)).isInstanceOf(BaseException.class)
                    .extracting("code").isEqualTo(503);
        }
    }

    @Test
    void rejectsRequestsAfterClosingSession() throws Exception {
        var classifier = new OnnxAlienClassifier(bundle(), mapper);
        classifier.start();
        classifier.close();
        assertThatThrownBy(() -> classifier.predict(() -> new byte[]{1})).isInstanceOf(BaseException.class)
                .extracting("code").isEqualTo(503);
    }

    private byte[] image() {
        try (var stream = getClass().getResourceAsStream("/alien-classifier/source-01.png")) {
            return stream.readAllBytes();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private AlienClassifierProperties bundle() throws Exception {
        for (String name : new String[]{"model.onnx", "labels.json", "preprocessing.json", "model-manifest.json"}) {
            String resource = name.equals("preprocessing.json") ? "/alien-classifier/" + name
                    : "/alien-classifier/model/" + name;
            try (var stream = getClass().getResourceAsStream(resource)) {
                Files.copy(stream, directory.resolve(name));
            }
        }
        var properties = new AlienClassifierProperties();
        properties.setModelPath(new FileSystemResource(directory.resolve("model.onnx")));
        return properties;
    }

    private void updateHash(String name) throws Exception {
        ObjectNode manifest = (ObjectNode) mapper.readTree(directory.resolve("model-manifest.json").toFile());
        ((ObjectNode) manifest.get("files")).put(name, HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(directory.resolve(name)))));
        mapper.writeValue(directory.resolve("model-manifest.json").toFile(), manifest);
    }
}
