package freshmart;

import java.math.BigDecimal;
import java.util.List;

/**
 * Concrete component of the Decorator pattern and step 1 of the final bill:
 * product total = sum of (unit price x quantity). It adds no discount, delivery fee or tax.
 */
public final class EUK00361749_ProductTotalBill implements EUK00361749_Bill {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final List<EUK00361749_OrderLine> lines;
    private final BigDecimal productTotal;

    public EUK00361749_ProductTotalBill(List<EUK00361749_OrderLine> lines) {
        this.lines = List.copyOf(lines);
        if (this.lines.isEmpty()) {
            throw new IllegalArgumentException("An order must contain at least one product.");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (EUK00361749_OrderLine line : this.lines) {
            total = total.add(line.lineTotal());
        }
        this.productTotal = EUK00361749_RatePricingStrategy.roundMoney(total);
    }

    @Override
    public List<EUK00361749_OrderLine> getLines() {
        return lines;
    }

    @Override
    public BigDecimal getProductTotal() {
        return productTotal;
    }

    @Override
    public BigDecimal getDiscount() {
        return ZERO;
    }

    @Override
    public BigDecimal getDiscountFor(EUK00361749_OrderLine line) {
        return ZERO;
    }

    @Override
    public BigDecimal getDeliveryFee() {
        return ZERO;
    }

    @Override
    public BigDecimal getTax() {
        return ZERO;
    }

    @Override
    public BigDecimal getAmount() {
        return productTotal;
    }

    @Override
    public String describeLayers() {
        return "Product total";
    }
}
