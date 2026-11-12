package freshmart;

import java.util.Locale;

/**
 * Shared input checks used by every class that accepts user data, so each rule is written once.
 * These check the format only; they do not prove that a NIC or email address really exists.
 */
public final class EUK00361749_Validation {

    private EUK00361749_Validation() {
        // Utility class: no objects are needed.
    }

    /**
     * Requires non-blank printable text of at most {@code maxLength} characters.
     *
     * @return the trimmed text
     */
    public static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank() || value.trim().length() > maxLength
                || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(fieldName + " must contain 1-" + maxLength + " printable characters.");
        }
        return value.trim();
    }

    /** @return the email in lower case, e.g. name@example.com */
    public static String validateEmail(String value) {
        String email = requireText(value, "Email", 254).toLowerCase(Locale.ROOT);
        if (!email.matches("[^\\s@]+@[^\\s@]+[.][^\\s@]+")) {
            throw new IllegalArgumentException("Enter a valid email address, e.g. name@example.com.");
        }
        return email;
    }

    /** Sri Lankan NIC: old format 9 digits + V/X, or new format 12 digits. */
    public static String validateNic(String value) {
        String nic = requireText(value, "NIC", 12).toUpperCase(Locale.ROOT);
        if (!nic.matches("([0-9]{9}[VX]|[0-9]{12})")) {
            throw new IllegalArgumentException("NIC must be 9 digits followed by V/X, or 12 digits.");
        }
        return nic;
    }

    /** 7-15 digits with an optional leading "+". */
    public static String validatePhone(String value) {
        String phone = requireText(value, "Contact number", 16);
        if (!phone.matches("[+]?[0-9]{7,15}")) {
            throw new IllegalArgumentException("Contact number must be 7-15 digits (optional leading +).");
        }
        return phone;
    }

    /** Product IDs use letters, digits and hyphens and are stored in upper case. */
    public static String validateProductId(String value) {
        String productId = requireText(value, "Product ID", 20).toUpperCase(Locale.ROOT);
        if (!productId.matches("[A-Z0-9-]+")) {
            throw new IllegalArgumentException("Product ID may only contain letters, digits and hyphens.");
        }
        return productId;
    }
}
