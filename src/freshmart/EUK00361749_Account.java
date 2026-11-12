package freshmart;

/**
 * Abstract user account shared by customers and the administrator.
 *
 * <p>Common identity fields and password checking are inherited by both subclasses. Each subclass
 * overrides {@link #getRole()} and {@link #getMenuTitle()}, so the console can treat any account
 * polymorphically.
 */
public abstract class EUK00361749_Account {

    private final String accountId;
    private final String name;
    private final String email;
    private final String passwordHash;

    /**
     * @param accountId    unique key (NIC for customers, "ADMIN" for the administrator)
     * @param name         display name
     * @param email        login email (stored in lower case)
     * @param passwordHash PBKDF2 hash produced by {@link EUK00361749_Password#hash(char[])}
     */
    protected EUK00361749_Account(String accountId, String name, String email, String passwordHash) {
        this.accountId = EUK00361749_Validation.requireText(accountId, "Account ID", 40);
        this.name = EUK00361749_Validation.requireText(name, "Name", 100);
        this.email = EUK00361749_Validation.validateEmail(email);
        this.passwordHash = EUK00361749_Validation.requireText(passwordHash, "Password hash", 200);
    }

    public final String getAccountId() {
        return accountId;
    }

    public final String getName() {
        return name;
    }

    public final String getEmail() {
        return email;
    }

    /** Package-private: only the store (for saving) and service (for profile edits) need it. */
    final String getPasswordHash() {
        return passwordHash;
    }

    /** @return true when the password matches this account's stored hash. */
    public final boolean checkPassword(char[] password) {
        return EUK00361749_Password.verify(password, passwordHash);
    }

    /** @return "ADMIN" or "CUSTOMER". */
    public abstract String getRole();

    /** @return the heading shown above this account's menu. */
    public abstract String getMenuTitle();
}
