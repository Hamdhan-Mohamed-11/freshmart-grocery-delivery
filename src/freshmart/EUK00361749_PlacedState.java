package freshmart;

/**
 * Concrete state "Placed". The order has been accepted and stock reserved. It may move on to Preparing or be cancelled.
 */
public final class EUK00361749_PlacedState extends EUK00361749_OrderState {

    public EUK00361749_PlacedState() {
        super("PLACED", "Placed");
    }

    @Override
    public EUK00361749_OrderState advance() {
        return new EUK00361749_PreparingState();
    }

    @Override
    public EUK00361749_OrderState cancel() {
        return new EUK00361749_CancelledState();
    }

    @Override
    public boolean isCancellable() {
        return true;
    }
}
