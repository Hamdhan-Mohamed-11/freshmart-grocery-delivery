package freshmart;

import java.math.BigDecimal;
import java.util.Objects;

/** Immutable record; stock status is derived so status and quantity cannot disagree. */
public record EUK00361749_Product(
    String id, String name, EUK00361749_Category category, BigDecimal price, int quantity) {
  public static final int LOW_STOCK_LIMIT = 5;

  public EUK00361749_Product {
    id = EUK00361749_Validation.productId(id);
    name = EUK00361749_Validation.text(name, "Product name", 100);
    Objects.requireNonNull(category);
    if (price == null
        || price.signum() <= 0
        || price.scale() > 2
        || price.compareTo(new BigDecimal("1000000")) > 0)
      throw new IllegalArgumentException(
          "Price must be positive, at most LKR 1000000, with at most 2 decimals.");
    price = price.setScale(2);
    if (quantity < 0) throw new IllegalArgumentException("Stock cannot be negative.");
  }

  public EUK00361749_StockStatus stockStatus() {
    return quantity == 0
        ? EUK00361749_StockStatus.OUT_OF_STOCK
        : quantity <= LOW_STOCK_LIMIT
            ? EUK00361749_StockStatus.LOW_STOCK
            : EUK00361749_StockStatus.IN_STOCK;
  }

  public EUK00361749_Product withQuantity(int q) {
    return new EUK00361749_Product(id, name, category, price, q);
  }

  @Override
  public String toString() {
    return id
        + " | "
        + name
        + " | "
        + category
        + " | LKR "
        + price
        + " | qty "
        + quantity
        + " | "
        + stockStatus();
  }
}
