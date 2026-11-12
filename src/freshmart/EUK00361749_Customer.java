package freshmart;

/** Immutable profile. The NIC is the stable account ID. */
public final class EUK00361749_Customer extends EUK00361749_Account {
  private final String phone, address;

  public EUK00361749_Customer(
      String nic, String name, String phone, String address, String email, String hash) {
    super(EUK00361749_Validation.nic(nic), name, email, hash);
    this.phone = EUK00361749_Validation.phone(phone);
    this.address = EUK00361749_Validation.text(address, "Address", 300);
  }

  public String phone() {
    return phone;
  }

  public String address() {
    return address;
  }

  @Override
  public String role() {
    return "CUSTOMER";
  }

  @Override
  public String toString() {
    return id() + " | " + name() + " | " + phone + " | " + address + " | " + email();
  }
}
