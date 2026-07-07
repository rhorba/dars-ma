package ma.darsma.backend.matching;

public interface EmbeddingProvider {

    int DIMENSIONS = 384;

    float[] embed(String text);
}
