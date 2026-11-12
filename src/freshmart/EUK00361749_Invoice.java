package freshmart;

import java.math.BigDecimal;

/** Immutable result from the pricing strategy. */
public record EUK00361749_Invoice(
    BigDecimal productTotal,
    BigDecimal discount,
    BigDecimal delivery,
    BigDecimal tax,
    BigDecimal payable,
    String detail) {
  @Override
  public String toString() {
    return detail
        + "\nProduct total: LKR "
        + productTotal
        + "\nDiscount: LKR "
        + discount
        + "\nDelivery (untaxed): LKR "
        + delivery
        + "\nTax: LKR "
        + tax
        + "\nFINAL PAYABLE: LKR "
        + payable;
  }
}
