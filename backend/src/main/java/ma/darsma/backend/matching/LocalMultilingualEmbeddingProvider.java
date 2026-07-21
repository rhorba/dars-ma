package ma.darsma.backend.matching;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.huggingface.translator.TextEmbeddingTranslator;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

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
        try {
            // DJL's curated HuggingFace model zoo (djl://ai.djl.huggingface.pytorch/<name>) only
            // serves this model's PyTorch weights, not an ONNX export - loading it with
            // .optEngine("OnnxRuntime") failed with "'.onnx' file not found" in the downloaded
            // bundle. The model repo does publish a real ONNX export
            // (.../resolve/main/onnx/model.onnx), but DJL's own URL-based downloader derives the
            // local filename from the (HF CDN-redirected) URL and loses the .onnx extension, so
            // OrtModel.load() can't find it either. Downloading it ourselves with an explicit
            // filename and pointing Criteria at that local path sidesteps both issues. The
            // tokenizer + mean-pooling translator (the standard sentence-transformers embedding
            // recipe) are built explicitly too, since bypassing the zoo also bypasses its
            // translator wiring.
            Path modelDir = Path.of(cacheDir, "onnx-model");
            Files.createDirectories(modelDir);
            Path onnxFile = modelDir.resolve("model.onnx");
            if (!Files.exists(onnxFile) || Files.size(onnxFile) == 0) {
                downloadFile("https://huggingface.co/" + modelName + "/resolve/main/onnx/model.onnx", onnxFile);
            }

            HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance(modelName);
            // The model's ONNX export declares 3 inputs (input_ids, attention_mask,
            // token_type_ids) - the translator omits token_type_ids unless told otherwise,
            // which fails at inference with "Input mismatch, looking for: [input_ids,
            // attention_mask, token_type_ids]" (OrtSymbolBlock requires an exact input match).
            TextEmbeddingTranslator translator = TextEmbeddingTranslator.builder(tokenizer)
                    .optPoolingMode("mean")
                    .optNormalize(true)
                    .optIncludeTokenTypes(true)
                    .build();
            Criteria<String, float[]> criteria = Criteria.builder()
                    .setTypes(String.class, float[].class)
                    .optApplication(Application.NLP.TEXT_EMBEDDING)
                    .optModelPath(modelDir)
                    .optEngine("OnnxRuntime")
                    .optTranslator(translator)
                    .build();
            this.model = criteria.loadModel();
        } catch (IOException | ModelNotFoundException | MalformedModelException | InterruptedException e) {
            throw new EmbeddingGenerationException("Failed to load embedding model " + modelName, e);
        }
    }

    private static void downloadFile(String url, Path target) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(target));
        if (response.statusCode() != 200) {
            throw new IOException("Failed to download " + url + ": HTTP " + response.statusCode());
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
