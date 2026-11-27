package freshmart;

import java.math.BigDecimal;

/**
 * Step 4 of the final bill: adds tax according to the product categories. Each line is taxed at
 * its category strategy's rate on the line value after that line's discount. The delivery fee is
 * a service charge and is not taxed (stated assumption).
 */
public final class EUK00361749_TaxDecorator extends EUK00361749_BillDecorator {

    private final BigDecimal tax;

    public EUK00361749_TaxDecorator(EUK00361749_Bill wrappedBill, EUK00361749_PricingCatalogue catalogue) {
        super(wrappedBill, catalogue);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (EUK00361749_OrderLine line : wrappedBill.getLines()) {
            BigDecimal taxableValue = line.lineTotal().subtract(wrappedBill.getDiscountFor(line));
            total = total.add(strategyFor(line).calculateTax(taxableValue));
        }
        this.tax = total;
    }

    @Override
    protected String getLayerName() {
        return "Tax";
    }

    @Override
    public BigDecimal getTax() {
        return getWrappedBill().getTax().add(tax);
    }

    @Override
    public BigDecimal getAmount() {
        return getWrappedBill().getAmount().add(tax);
    }
}
