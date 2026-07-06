package ma.darsma.backend.shared.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentEncryptionServiceTest {

    private static final String VALID_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void encryptThenDecrypt_returnsOriginalPlaintext() {
        DocumentEncryptionService service = new DocumentEncryptionService(VALID_KEY);
        byte[] plaintext = "diploma-content".getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = service.encrypt(plaintext);
        byte[] decrypted = service.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
        assertThat(encrypted).isNotEqualTo(plaintext);
    }

    @Test
    void encrypt_producesDifferentCiphertextEachTimeDueToRandomIv() {
        DocumentEncryptionService service = new DocumentEncryptionService(VALID_KEY);
        byte[] plaintext = "same-content".getBytes(StandardCharsets.UTF_8);

        byte[] first = service.encrypt(plaintext);
        byte[] second = service.encrypt(plaintext);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void constructor_rejectsKeyOfWrongLength() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new DocumentEncryptionService(shortKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
