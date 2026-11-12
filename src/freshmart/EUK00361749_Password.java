package freshmart;

import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Salted PBKDF2 records. No plaintext password is written to disk. */
public final class EUK00361749_Password {
  private static final int ITERATIONS = 600000;

  private EUK00361749_Password() {}

  public static String hash(char[] password) {
    if (password.length < 15 || password.length > 128)
      throw new IllegalArgumentException("Password must contain 15-128 characters.");
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    return ITERATIONS
        + ":"
        + Base64.getEncoder().encodeToString(salt)
        + ":"
        + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
  }

  public static boolean verify(char[] password, String stored) {
    try {
      String[] p = stored.split(":", -1);
      if (p.length != 3) return false;
      int count = Integer.parseInt(p[0]);
      if (p.length != 3 || count != ITERATIONS || password.length > 128) return false;
      byte[] salt = Base64.getDecoder().decode(p[1]), expected = Base64.getDecoder().decode(p[2]);
      return salt.length == 16
          && expected.length == 32
          && MessageDigest.isEqual(expected, derive(password, salt, count));
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  private static byte[] derive(char[] password, byte[] salt, int count) {
    PBEKeySpec spec = new PBEKeySpec(password, salt, count, 256);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("PBKDF2 unavailable.", ex);
    } finally {
      spec.clearPassword();
    }
  }
}
