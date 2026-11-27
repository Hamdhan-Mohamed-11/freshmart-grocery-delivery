package freshmart;

import java.util.List;
import java.util.Objects;

/**
 * Assembles the decorated bill in the order given by the case study and turns it into an invoice.
 *
 * <pre>
 *   Tax( DeliveryFee( Discount( ProductTotal(lines) ) ) )
 *   step 1 product total, step 2 discount, step 3 delivery fee, step 4 tax, step 5 final payable
 * </pre>
 */
public final class EUK00361749_BillCalculator {

    private final EUK00361749_PricingCatalogue catalogue;

    public EUK00361749_BillCalculator(EUK00361749_PricingCatalogue catalogue) {
        this.catalogue = Objects.requireNonNull(catalogue, "Pricing catalogue is required.");
    }

    /** @return the fully decorated bill for the given lines. */
    public EUK00361749_Bill buildBill(List<EUK00361749_OrderLine> lines) {
        EUK00361749_Bill bill = new EUK00361749_ProductTotalBill(lines);    // step 1
        bill = new EUK00361749_DiscountDecorator(bill, catalogue);          // step 2
        bill = new EUK00361749_DeliveryFeeDecorator(bill, catalogue);       // step 3
        bill = new EUK00361749_TaxDecorator(bill, catalogue);               // step 4
        return bill;                                                        // step 5: getAmount()
    }

    /** @return the invoice amounts for the given lines. */
    public EUK00361749_Invoice calculate(List<EUK00361749_OrderLine> lines) {
        return EUK00361749_Invoice.fromBill(buildBill(lines));
    }
}
