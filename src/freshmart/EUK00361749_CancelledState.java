package freshmart;

/**
 * Concrete state "Cancelled". Final state: the order was cancelled before Out for Delivery and its stock returned. No
 * transition is allowed.
 */
public final class EUK00361749_CancelledState extends EUK00361749_OrderState {

    public EUK00361749_CancelledState() {
        super("CANCELLED", "Cancelled");
    }

    @Override
    public boolean isFinal() {
        return true;
    }
}
