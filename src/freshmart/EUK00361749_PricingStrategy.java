package freshmart;

import java.math.BigDecimal;

/**
 * Strategy interface for the category-specific charges in Table 1 of the case study.
 *
 * <p>The billing decorators ask a strategy for a delivery fee, a discount or a tax without knowing
 * which category, or which rates, they are dealing with. A new category or a changed rate is
 * therefore a new strategy object registered in {@link EUK00361749_PricingCatalogue}; the billing
 * code itself does not change.
 */
public interface EUK00361749_PricingStrategy {

    /** @return the delivery fee charged for this category (LKR). */
    BigDecimal getDeliveryFee();

    /** @return the smallest product total accepted for an order containing this category (LKR). */
    BigDecimal getMinimumOrderValue();

    /**
     * @param productValue value of this category's goods (unit price x quantity)
     * @return the category discount on that value, rounded to cents
     */
    BigDecimal calculateDiscount(BigDecimal productValue);

    /**
     * @param taxableValue value of this category's goods after any discount
     * @return the category tax on that value, rounded to cents
     */
    BigDecimal calculateTax(BigDecimal taxableValue);

    /** @return a one-line summary of the rates, used in listings and evidence. */
    String describe();
}
