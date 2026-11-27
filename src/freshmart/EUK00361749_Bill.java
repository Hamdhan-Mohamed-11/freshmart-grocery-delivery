package freshmart;

import java.math.BigDecimal;
import java.util.List;

/**
 * Component interface of the Decorator pattern: one layer of an order's bill.
 *
 * <p>The innermost layer is {@link EUK00361749_ProductTotalBill} (step 1). Each decorator wraps
 * the previous layer, adds one charge or deduction, and passes every other value through
 * unchanged, so the final bill is assembled in the exact order of the case study:
 * product total, discount, delivery fee, tax, final payable amount.
 */
public interface EUK00361749_Bill {

    /** @return the product lines being billed. */
    List<EUK00361749_OrderLine> getLines();

    /** @return step 1: sum of unit price x quantity. */
    BigDecimal getProductTotal();

    /** @return step 2: total discount (zero until the discount layer is added). */
    BigDecimal getDiscount();

    /** @return the discount given on one line (used by the tax layer). */
    BigDecimal getDiscountFor(EUK00361749_OrderLine line);

    /** @return step 3: delivery fee (zero until the delivery layer is added). */
    BigDecimal getDeliveryFee();

    /** @return step 4: tax (zero until the tax layer is added). */
    BigDecimal getTax();

    /** @return the running amount after this layer; for the outermost layer, step 5. */
    BigDecimal getAmount();

    /** @return the name of every layer from the innermost outwards, e.g. "Product total > Discount". */
    String describeLayers();
}
