package freshmart;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Abstract decorator: holds the wrapped bill and forwards every call to it. Concrete decorators
 * override only the value their step changes and add their charge to {@link #getAmount()}.
 */
public abstract class EUK00361749_BillDecorator implements EUK00361749_Bill {

    private final EUK00361749_Bill wrappedBill;
    private final EUK00361749_PricingCatalogue catalogue;

    protected EUK00361749_BillDecorator(EUK00361749_Bill wrappedBill, EUK00361749_PricingCatalogue catalogue) {
        this.wrappedBill = Objects.requireNonNull(wrappedBill, "A decorator must wrap a bill.");
        this.catalogue = Objects.requireNonNull(catalogue, "Pricing catalogue is required.");
    }

    /** @return the layer this decorator wraps. */
    protected final EUK00361749_Bill getWrappedBill() {
        return wrappedBill;
    }

    /** @return the pricing strategy for one line's category (Strategy pattern). */
    protected final EUK00361749_PricingStrategy strategyFor(EUK00361749_OrderLine line) {
        return catalogue.strategyFor(line.category());
    }

    /** @return the name of this decorator's step, e.g. "Discount". */
    protected abstract String getLayerName();

    @Override
    public List<EUK00361749_OrderLine> getLines() {
        return wrappedBill.getLines();
    }

    @Override
    public BigDecimal getProductTotal() {
        return wrappedBill.getProductTotal();
    }

    @Override
    public BigDecimal getDiscount() {
        return wrappedBill.getDiscount();
    }

    @Override
    public BigDecimal getDiscountFor(EUK00361749_OrderLine line) {
        return wrappedBill.getDiscountFor(line);
    }

    @Override
    public BigDecimal getDeliveryFee() {
        return wrappedBill.getDeliveryFee();
    }

    @Override
    public BigDecimal getTax() {
        return wrappedBill.getTax();
    }

    @Override
    public BigDecimal getAmount() {
        return wrappedBill.getAmount();
    }

    @Override
    public String describeLayers() {
        return wrappedBill.describeLayers() + " > " + getLayerName();
    }
}
