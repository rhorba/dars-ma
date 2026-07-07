package ma.darsma.backend.matching;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the real embedding model end-to-end (downloads ~120-470MB from huggingface.co on
 * first run). Tagged "real-model" and excluded from the default `mvn verify` run (see pom.xml) so
 * CI doesn't depend on that external network call — run manually to verify the model integration.
 */
@Tag("real-model")
class LocalMultilingualEmbeddingProviderIT {

    private static LocalMultilingualEmbeddingProvider provider;

    @BeforeAll
    static void setUp() {
        provider = new LocalMultilingualEmbeddingProvider(
                "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
                System.getProperty("java.io.tmpdir") + "/dars-ma-djl-cache-it");
    }

    @AfterAll
    static void tearDown() {
        provider.close();
    }

    @Test
    void embed_returns384DimensionalVector() {
        float[] vector = provider.embed("Je cherche un professeur de mathématiques");
        assertThat(vector).hasSize(384);
    }

    @Test
    void embed_similarMultilingualSentencesAreCloserThanUnrelatedOnes() {
        float[] frenchMath = provider.embed("Je cherche un professeur de mathématiques pour le lycée");
        float[] englishMath = provider.embed("I am looking for a high school math tutor");
        float[] arabicCooking = provider.embed("أبحث عن وصفة لطبخ الكسكس");

        double sameTopicDifferentLanguage = cosineSimilarity(frenchMath, englishMath);
        double differentTopic = cosineSimilarity(frenchMath, arabicCooking);

        assertThat(sameTopicDifferentLanguage).isGreaterThan(differentTopic);
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
