package freshmart;

public final class EUK00361749_Admin extends EUK00361749_Account {
  public EUK00361749_Admin(String name, String email, String hash) {
    super("ADMIN", name, email, hash);
  }

  @Override
  public String role() {
    return "ADMIN";
  }
}
