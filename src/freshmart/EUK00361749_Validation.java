package freshmart;

import java.util.Locale;

/** Central format checks; these do not verify identity or email ownership. */
public final class EUK00361749_Validation {
  private EUK00361749_Validation() {}

  public static String text(String s, String field, int max) {
    if (s == null || s.isBlank() || s.length() > max || s.chars().anyMatch(Character::isISOControl))
      throw new IllegalArgumentException(
          field + " must contain 1-" + max + " printable characters.");
    return s.trim();
  }

  public static String email(String s) {
    s = text(s, "Email", 254).toLowerCase(Locale.ROOT);
    if (!s.matches("[^\\s@]+@[^\\s@]+[.][^\\s@]+"))
      throw new IllegalArgumentException("Invalid email.");
    return s;
  }

  public static String nic(String s) {
    s = text(s, "NIC", 12).toUpperCase(Locale.ROOT);
    if (!s.matches("([0-9]{9}[VX]|[0-9]{12})"))
      throw new IllegalArgumentException("NIC: 9 digits + V/X, or 12 digits.");
    return s;
  }

  public static String phone(String s) {
    s = text(s, "Phone", 16);
    if (!s.matches("[+]?[0-9]{7,15}"))
      throw new IllegalArgumentException("Phone: 7-15 digits with optional +.");
    return s;
  }

  public static String productId(String s) {
    s = text(s, "Product ID", 20).toUpperCase(Locale.ROOT);
    if (!s.matches("[A-Z0-9-]+"))
      throw new IllegalArgumentException("Product ID: letters, digits and hyphens.");
    return s;
  }
}
