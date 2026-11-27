package freshmart;

/**
 * Concrete state "Preparing". Assumed intermediate status: items are being picked and packed. The order may still be
 * cancelled, or move on to Out for Delivery.
 */
public final class EUK00361749_PreparingState extends EUK00361749_OrderState {

    public EUK00361749_PreparingState() {
        super("PREPARING", "Preparing");
    }

    @Override
    public EUK00361749_OrderState advance() {
        return new EUK00361749_OutForDeliveryState();
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
