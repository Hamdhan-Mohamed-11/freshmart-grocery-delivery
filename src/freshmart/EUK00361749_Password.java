package freshmart;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing with salted PBKDF2-HMAC-SHA256. Only the hash is saved, never the password.
 * Stored format: {@code iterations:base64(salt):base64(hash)}.
 */
public final class EUK00361749_Password {

    static final int ITERATIONS = 120_000;
    public static final int MINIMUM_LENGTH = 8;
    public static final int MAXIMUM_LENGTH = 128;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

    private EUK00361749_Password() {
        // Utility class: no objects are needed.
    }

    /** Hashes a new password with a fresh random salt. */
    public static String hash(char[] password) {
        if (password.length < MINIMUM_LENGTH || password.length > MAXIMUM_LENGTH) {
            throw new IllegalArgumentException("Password must contain " + MINIMUM_LENGTH + "-" + MAXIMUM_LENGTH + " characters.");
        }
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(deriveKey(password, salt, ITERATIONS));
    }

    /** @return true if the password matches the stored hash; false for any malformed record. */
    public static boolean verify(char[] password, String storedHash) {
        try {
            String[] parts = storedHash.split(":", -1);
            if (parts.length != 3 || password.length > MAXIMUM_LENGTH) {
                return false;
            }
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);
            if (iterations != ITERATIONS || salt.length != SALT_BYTES || expectedHash.length != HASH_BITS / 8) {
                return false;
            }
            // Constant-time comparison so timing does not reveal how many bytes matched.
            return MessageDigest.isEqual(expectedHash, deriveKey(password, salt, iterations));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static byte[] deriveKey(char[] password, byte[] salt, int iterations) {
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("PBKDF2 is not available on this JVM.", ex);
        } finally {
            keySpec.clearPassword();
        }
    }
}
