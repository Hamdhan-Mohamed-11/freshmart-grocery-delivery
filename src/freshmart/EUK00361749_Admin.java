package freshmart;

/** The single FreshMart administrator, who manages products, stock and delivery progress. */
public final class EUK00361749_Admin extends EUK00361749_Account {

    /** Fixed account ID: there is exactly one administrator account. */
    public static final String ADMIN_ACCOUNT_ID = "ADMIN";

    public EUK00361749_Admin(String name, String email, String passwordHash) {
        super(ADMIN_ACCOUNT_ID, name, email, passwordHash);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public String getMenuTitle() {
        return "ADMINISTRATOR MENU - " + getName();
    }
}
