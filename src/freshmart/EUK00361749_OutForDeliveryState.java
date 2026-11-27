package freshmart;

/**
 * Concrete state "Out for Delivery". The order has left the store. It can only become Delivered; cancellation is refused by the
 * inherited {@link EUK00361749_OrderState#cancel()}.
 */
public final class EUK00361749_OutForDeliveryState extends EUK00361749_OrderState {

    public EUK00361749_OutForDeliveryState() {
        super("OUT_FOR_DELIVERY", "Out for Delivery");
    }

    @Override
    public EUK00361749_OrderState advance() {
        return new EUK00361749_DeliveredState();
    }
}
