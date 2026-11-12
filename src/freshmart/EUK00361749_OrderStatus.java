package freshmart;

public enum EUK00361749_OrderStatus {
  PLACED,
  PREPARING,
  OUT_FOR_DELIVERY,
  DELIVERED,
  CANCELLED;

  public boolean cancellable() {
    return this == PLACED || this == PREPARING;
  }

  public EUK00361749_OrderStatus next() {
    return switch (this) {
      case PLACED -> PREPARING;
      case PREPARING -> OUT_FOR_DELIVERY;
      case OUT_FOR_DELIVERY -> DELIVERED;
      default -> throw new IllegalArgumentException("Terminal order status cannot advance.");
    };
  }
}
