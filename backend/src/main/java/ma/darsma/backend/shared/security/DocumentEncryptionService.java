package ma.darsma.backend.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM, application-layer encryption for data stored in BYTEA columns (verification documents).
 * Ciphertext layout: [12-byte IV][GCM ciphertext+tag] so each blob is self-contained for decryption.
 */
@Service
public class DocumentEncryptionService {

    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public DocumentEncryptionService(@Value("${verification.doc-encryption-key}") String base64Key) {
        byte[] rawKey = Base64.getDecoder().decode(base64Key);
        if (rawKey.length != 32) {
            throw new IllegalStateException("verification.doc-encryption-key must decode to exactly 32 bytes (AES-256)");
        }
        this.key = new SecretKeySpec(rawKey, "AES");
    }

    public byte[] encrypt(byte[] plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);
            return ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt document", e);
        }
    }

    public byte[] decrypt(byte[] blob) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(blob);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt document", e);
        }
    }
}
