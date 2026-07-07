package ma.darsma.backend.matching;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Generates real semantic embeddings using a local multilingual sentence-transformer model
 * (covers FR/AR/EN) run in-process via DJL + ONNX Runtime — no external API, no per-call cost.
 * The model is downloaded from HuggingFace on first use and cached under {@code embedding.cache-dir}
 * (mounted as a Docker volume in prod so it survives container restarts).
 */
@Component
@Profile("!test")
public class LocalMultilingualEmbeddingProvider implements EmbeddingProvider {

    private final ZooModel<String, float[]> model;

    public LocalMultilingualEmbeddingProvider(
            @Value("${embedding.model-name}") String modelName,
            @Value("${embedding.cache-dir}") String cacheDir) {
        System.setProperty("DJL_CACHE_DIR", cacheDir);
        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optApplication(Application.NLP.TEXT_EMBEDDING)
                .optModelUrls("djl://ai.djl.huggingface.onnxruntime/" + modelName)
                .optEngine("OnnxRuntime")
                .build();
        try {
            this.model = criteria.loadModel();
        } catch (IOException | ModelNotFoundException | MalformedModelException e) {
            throw new EmbeddingGenerationException("Failed to load embedding model " + modelName, e);
        }
    }

    @Override
    public float[] embed(String text) {
        try (Predictor<String, float[]> predictor = model.newPredictor()) {
            return predictor.predict(text);
        } catch (TranslateException e) {
            throw new EmbeddingGenerationException("Failed to generate embedding", e);
        }
    }

    @PreDestroy
    void close() {
        model.close();
    }
}
