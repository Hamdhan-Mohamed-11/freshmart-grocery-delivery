package freshmart;

import java.time.LocalDate;
import java.util.*;

/** Historical profile and immutable line snapshots belong to this order. */
public record EUK00361749_Order(
    String id,
    String customerId,
    String customerDetails,
    LocalDate deliveryDate,
    EUK00361749_TimeSlot slot,
    EUK00361749_OrderStatus status,
    List<EUK00361749_OrderLine> lines) {
  public EUK00361749_Order {
    id = EUK00361749_Validation.text(id, "Order ID", 40);
    customerId = EUK00361749_Validation.nic(customerId);
    customerDetails = EUK00361749_Validation.text(customerDetails, "Customer details", 700);
    Objects.requireNonNull(deliveryDate);
    Objects.requireNonNull(slot);
    Objects.requireNonNull(status);
    lines = List.copyOf(lines);
    if (lines.isEmpty()) throw new IllegalArgumentException("Order needs at least one product.");
    Set<String> seen = new HashSet<>();
    for (EUK00361749_OrderLine line : lines)
      if (!seen.add(line.productId())) throw new IllegalArgumentException("Duplicate order line.");
  }

  public EUK00361749_Order withStatus(EUK00361749_OrderStatus next) {
    return new EUK00361749_Order(id, customerId, customerDetails, deliveryDate, slot, next, lines);
  }

  @Override
  public String toString() {
    return id + " | " + customerId + " | " + deliveryDate + " | " + slot + " | " + status;
  }
}
