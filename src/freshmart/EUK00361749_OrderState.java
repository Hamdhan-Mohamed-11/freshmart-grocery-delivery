package freshmart;

/**
 * State pattern: the abstract state of an order in its delivery lifecycle.
 *
 * <pre>
 *   Placed --advance--> Preparing --advance--> Out for Delivery --advance--> Delivered
 *     |                    |
 *     +------cancel--------+-----> Cancelled      (cancel is refused from Out for Delivery onwards)
 * </pre>
 *
 * <p>The order (the context) never checks its own status with if/switch statements. It asks its
 * current state object to {@link #advance()} or {@link #cancel()}; each concrete state answers with
 * the next state or refuses the transition. By default every transition is refused, so a concrete
 * state only overrides the transitions it allows. States hold no data, so they are compared by
 * class.
 */
public abstract class EUK00361749_OrderState {

    private final String code;
    private final String displayName;

    /**
     * @param code        stable name saved in the data file, e.g. OUT_FOR_DELIVERY
     * @param displayName text shown to users, e.g. "Out for Delivery"
     */
    protected EUK00361749_OrderState(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /** @return the state that follows this one in delivery; refused unless overridden. */
    public EUK00361749_OrderState advance() {
        throw new IllegalStateException("A " + displayName + " order cannot move to another status.");
    }

    /** @return the Cancelled state; refused unless overridden. */
    public EUK00361749_OrderState cancel() {
        throw new IllegalStateException("The order is " + displayName + " and can no longer be cancelled.");
    }

    /** @return true while the customer may still cancel (before Out for Delivery). */
    public boolean isCancellable() {
        return false;
    }

    /** @return true for Delivered and Cancelled, which allow no further change. */
    public boolean isFinal() {
        return false;
    }

    public final String getCode() {
        return code;
    }

    /** Recreates a state from the code stored in the data file. */
    public static EUK00361749_OrderState fromCode(String code) {
        return switch (code) {
            case "PLACED" -> new EUK00361749_PlacedState();
            case "PREPARING" -> new EUK00361749_PreparingState();
            case "OUT_FOR_DELIVERY" -> new EUK00361749_OutForDeliveryState();
            case "DELIVERED" -> new EUK00361749_DeliveredState();
            case "CANCELLED" -> new EUK00361749_CancelledState();
            default -> throw new IllegalArgumentException("Unknown order status " + code + ".");
        };
    }

    @Override
    public final boolean equals(Object other) {
        return other != null && other.getClass() == getClass();
    }

    @Override
    public final int hashCode() {
        return code.hashCode();
    }

    @Override
    public final String toString() {
        return displayName;
    }
}
