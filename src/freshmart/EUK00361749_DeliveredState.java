package freshmart;

/**
 * Concrete state "Delivered". Final state: the order reached the customer. No transition is allowed.
 */
public final class EUK00361749_DeliveredState extends EUK00361749_OrderState {

    public EUK00361749_DeliveredState() {
        super("DELIVERED", "Delivered");
    }

    @Override
    public boolean isFinal() {
        return true;
    }
}
