package ma.darsma.backend.matching;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Deterministic stand-in for {@link LocalMultilingualEmbeddingProvider} so tests don't need to
 * download/run the real ONNX model. Same text always yields the same vector.
 */
@Component
@Profile("test")
public class FakeEmbeddingProvider implements EmbeddingProvider {

    @Override
    public float[] embed(String text) {
        Random random = new Random(text == null ? 0 : text.hashCode());
        float[] vector = new float[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            vector[i] = random.nextFloat();
        }
        return vector;
    }
}
