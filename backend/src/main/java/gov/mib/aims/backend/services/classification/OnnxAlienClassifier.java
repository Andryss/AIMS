package gov.mib.aims.backend.services.classification;

import ai.onnxruntime.OnnxJavaType;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.mib.aims.backend.config.AlienClassifierProperties;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.exception.BaseException;
import gov.mib.aims.backend.model.AlienPrediction;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.FloatBuffer;
import java.util.Map;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * Одна локальная CPU-сессия ONNX с ограниченным числом запросов без очереди ожидания.
 */
@Service
@Slf4j
public class OnnxAlienClassifier implements AlienClassifier, AutoCloseable {

    private static final int BATCH_SIZE = 1;
    private static final int INTER_OP_THREADS = 1;
    private static final int INPUT_COUNT = 1;
    private static final int OUTPUT_COUNT = 1;
    private static final int DYNAMIC_BATCH_SIZE = -1;

    private final AlienClassifierProperties properties;
    private final ObjectMapper mapper;
    private final AlienImagePreprocessor preprocessor;
    private final Semaphore requests;
    private volatile LoadedModel loaded;

    public OnnxAlienClassifier(AlienClassifierProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        this.preprocessor = new AlienImagePreprocessor(properties);
        this.requests = new Semaphore(properties.getMaxConcurrentRequests());
    }

    /**
     * Проверяет комплект и открывает сессию. Ошибка модели не мешает запуску приложения.
     */
    @PostConstruct
    public void start() {
        try {
            var model = AlienClassifierModel.load(properties.getModelPath(), mapper);
            var environment = OrtEnvironment.getEnvironment();
            try (var options = new OrtSession.SessionOptions()) {
                options.setIntraOpNumThreads(properties.getThreads());
                options.setInterOpNumThreads(INTER_OP_THREADS);
                var session = environment.createSession(model.weights(), options);
                try {
                    validateShapes(session, model);
                    loaded = new LoadedModel(session, model);
                } catch (Exception e) {
                    session.close();
                    throw e;
                }
            }
            log.info("Alien classifier loaded: {} classes", model.keys().size());
        } catch (Exception | LinkageError e) {
            log.warn("Alien classifier unavailable: {}", e.toString());
        }
    }

    @Override
    public AlienPrediction predict(List<Supplier<byte[]>> images) {
        if (!requests.tryAcquire()) {
            throw Errors.classifierUnavailable();
        }
        try {
            LoadedModel current = loaded;
            if (current == null) {
                throw Errors.classifierUnavailable();
            }
            var scores = new AlienClassificationScores(current.model().keys().size());
            for (var image : images) {
                float[] pixels;
                try {
                    pixels = preprocessor.prepare(image.get());
                } catch (BaseException e) {
                    if (e.getCode() != 400 && e.getCode() != 404) {
                        log.warn("Alien classifier image read failed", e);
                        throw Errors.classifierUnavailable();
                    }
                    log.info("Skipping unavailable or unsupported incident image: {}", e.getMessage());
                    continue;
                } catch (RuntimeException e) {
                    log.warn("Alien classifier image read failed", e);
                    throw Errors.classifierUnavailable();
                }
                try (var input = OnnxTensor.createTensor(OrtEnvironment.getEnvironment(), FloatBuffer.wrap(pixels),
                        new long[]{BATCH_SIZE, AlienImagePreprocessor.CHANNEL_COUNT,
                                AlienImagePreprocessor.CROP_SIZE, AlienImagePreprocessor.CROP_SIZE});
                        var result = current.session().run(Map.of(current.model().input(), input))) {
                    var logits = (float[][]) result.get(current.model().output()).orElseThrow().getValue();
                    if (logits.length != BATCH_SIZE) {
                        throw new IllegalStateException("Invalid classifier output batch");
                    }
                    scores.add(logits[0]);
                } catch (OrtException | ClassCastException | IllegalStateException e) {
                    log.warn("Alien classifier execution failed", e);
                    throw Errors.classifierUnavailable();
                }
            }
            if (scores.isEmpty()) {
                throw Errors.validationError("В инциденте нет доступных изображений PNG или JPEG");
            }
            return scores.prediction(current.model().keys(), current.model().threshold());
        } finally {
            requests.release();
        }
    }

    private void validateShapes(OrtSession session, AlienClassifierModel model) throws OrtException {
        var input = session.getInputInfo().get(model.input());
        var output = session.getOutputInfo().get(model.output());
        if (session.getNumInputs() != INPUT_COUNT || session.getNumOutputs() != OUTPUT_COUNT
                || input == null || output == null
                || !(input.getInfo() instanceof TensorInfo in) || !(output.getInfo() instanceof TensorInfo out)
                || in.type != OnnxJavaType.FLOAT || out.type != OnnxJavaType.FLOAT
                || !shape(in.getShape(), new long[]{AlienImagePreprocessor.CHANNEL_COUNT,
                        AlienImagePreprocessor.CROP_SIZE, AlienImagePreprocessor.CROP_SIZE})
                || !shape(out.getShape(), new long[]{model.keys().size()})) {
            throw new IllegalStateException("Invalid classifier tensor type or shape");
        }
    }

    private boolean shape(long[] actual, long[] dimensions) {
        if (actual.length != dimensions.length + 1 || (actual[0] != DYNAMIC_BATCH_SIZE && actual[0] != BATCH_SIZE)) {
            return false;
        }
        for (int index = 0; index < dimensions.length; index++) {
            if (actual[index + 1] != dimensions[index]) {
                return false;
            }
        }
        return true;
    }

    @Override
    @PreDestroy
    public void close() {
        requests.acquireUninterruptibly(properties.getMaxConcurrentRequests());
        try {
            LoadedModel current = loaded;
            loaded = null;
            if (current != null) {
                current.session().close();
            }
        } catch (OrtException e) {
            log.warn("Failed to close alien classifier", e);
        } finally {
            requests.release(properties.getMaxConcurrentRequests());
        }
    }

    private record LoadedModel(OrtSession session, AlienClassifierModel model) {
    }
}
