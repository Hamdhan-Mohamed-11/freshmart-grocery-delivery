package freshmart;

/**
 * Order lifecycle: PLACED -> PREPARING -> OUT_FOR_DELIVERY -> DELIVERED, or CANCELLED.
 * Customers may cancel only before the order is Out for Delivery.
 */
public enum EUK00361749_OrderStatus {
    PLACED("Placed"),
    PREPARING("Preparing"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private final String displayName;

    EUK00361749_OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    /** @return true while the order has not yet left for delivery. */
    public boolean isCancellable() {
        return this == PLACED || this == PREPARING;
    }

    /** @return the next stage of delivery; finished orders cannot move on. */
    public EUK00361749_OrderStatus nextStatus() {
        return switch (this) {
            case PLACED -> PREPARING;
            case PREPARING -> OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> DELIVERED;
            default -> throw new IllegalArgumentException("A " + displayName + " order cannot move to another status.");
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
