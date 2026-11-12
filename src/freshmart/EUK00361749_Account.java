package freshmart;

/** Abstract authenticated identity, shared by customers and the administrator. */
public abstract class EUK00361749_Account {
  private final String id, name, email, passwordHash;

  protected EUK00361749_Account(String id, String name, String email, String passwordHash) {
    this.id = EUK00361749_Validation.text(id, "Account ID", 40);
    this.name = EUK00361749_Validation.text(name, "Name", 100);
    this.email = EUK00361749_Validation.email(email);
    this.passwordHash = EUK00361749_Validation.text(passwordHash, "Credential", 200);
  }

  public final String id() {
    return id;
  }

  public final String name() {
    return name;
  }

  public final String email() {
    return email;
  }

  final String passwordHash() {
    return passwordHash;
  }

  public final boolean authenticates(char[] password) {
    return EUK00361749_Password.verify(password, passwordHash);
  }

  public abstract String role();
}
