package freshmart;

import java.math.*;
import java.util.*;

/** Category-aware Strategy, shared by checkout and invoice generation. */
public final class EUK00361749_CategoryPricing implements EUK00361749_PricingPolicy {
  private static BigDecimal money(BigDecimal amount) {
    return amount.setScale(2, RoundingMode.HALF_UP);
  }

  @Override
  public EUK00361749_Invoice calculate(List<EUK00361749_OrderLine> lines) {
    if (lines.isEmpty()) throw new IllegalArgumentException("Order needs at least one product.");
    BigDecimal total =
        lines.stream().map(EUK00361749_OrderLine::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal minimum =
        lines.stream().map(l -> l.category().minimum()).max(BigDecimal::compareTo).orElseThrow();
    if (total.compareTo(minimum) < 0)
      throw new IllegalArgumentException("Minimum product total is LKR " + minimum + ".");
    boolean eligible = total.compareTo(new BigDecimal("10000")) > 0;
    BigDecimal discount = BigDecimal.ZERO, tax = BigDecimal.ZERO;
    BigDecimal delivery =
        lines.stream().map(l -> l.category().delivery()).max(BigDecimal::compareTo).orElseThrow();
    StringBuilder detail = new StringBuilder("PRODUCT LINES (all amounts LKR)");
    for (EUK00361749_OrderLine l : lines) {
      BigDecimal d =
          money(eligible ? l.total().multiply(l.category().discount()) : BigDecimal.ZERO);
      BigDecimal t = money(l.total().subtract(d).multiply(l.category().tax()));
      discount = discount.add(d);
      tax = tax.add(t);
      detail
          .append("\n")
          .append(l.productId())
          .append(" | ")
          .append(l.name())
          .append(" | ")
          .append(l.category())
          .append("\n  Unit ")
          .append(l.unitPrice())
          .append(" x ")
          .append(l.quantity())
          .append(" = ")
          .append(l.total())
          .append(" | discount ")
          .append(d)
          .append(" | tax ")
          .append(t);
    }
    return new EUK00361749_Invoice(
        money(total),
        money(discount),
        money(delivery),
        money(tax),
        money(total.subtract(discount).add(delivery).add(tax)),
        detail.toString());
  }
}
