package freshmart;

/**
 * A registered customer. The NIC is the permanent account ID; the other details can be updated
 * (an update creates a new immutable Customer object with the same NIC).
 */
public final class EUK00361749_Customer extends EUK00361749_Account {

    private final String contactNumber;
    private final String deliveryAddress;

    /**
     * @param nic             Sri Lankan NIC (account ID)
     * @param name            full name
     * @param contactNumber   phone number
     * @param deliveryAddress where orders are delivered
     * @param email           login email
     * @param passwordHash    PBKDF2 hash
     */
    public EUK00361749_Customer(String nic, String name, String contactNumber, String deliveryAddress,
                                String email, String passwordHash) {
        super(EUK00361749_Validation.validateNic(nic), name, email, passwordHash);
        this.contactNumber = EUK00361749_Validation.validatePhone(contactNumber);
        this.deliveryAddress = EUK00361749_Validation.requireText(deliveryAddress, "Delivery address", 300);
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    @Override
    public String getRole() {
        return "CUSTOMER";
    }

    @Override
    public String getMenuTitle() {
        return "CUSTOMER MENU - " + getName();
    }

    @Override
    public String toString() {
        return "NIC " + getAccountId() + " | " + getName() + " | " + contactNumber + " | "
                + deliveryAddress + " | " + getEmail();
    }
}
