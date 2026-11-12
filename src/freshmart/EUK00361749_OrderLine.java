package freshmart;

import java.math.BigDecimal;
import java.util.Objects;

/** An order-owned product snapshot; later inventory edits cannot change it. */
public record EUK00361749_OrderLine(
    String productId,
    String name,
    EUK00361749_Category category,
    BigDecimal unitPrice,
    int quantity) {
  public EUK00361749_OrderLine {
    EUK00361749_Product valid = new EUK00361749_Product(productId, name, category, unitPrice, 0);
    productId = valid.id();
    name = valid.name();
    unitPrice = valid.price();
    Objects.requireNonNull(category);
    if (quantity <= 0) throw new IllegalArgumentException("Order quantity must be positive.");
  }

  public BigDecimal total() {
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
  }
}
