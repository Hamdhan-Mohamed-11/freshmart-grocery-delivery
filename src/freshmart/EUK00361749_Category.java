package freshmart;

import java.math.BigDecimal;

/**
 * Product categories with the charges from Table 1 of the case study. Keeping the values on the
 * enum means pricing data lives in exactly one place.
 */
public enum EUK00361749_Category {

    //                    display name            delivery  minimum  discount  tax
    VEGETABLES("Vegetables", 300, 2000, "0.05", "0.05"),
    FRUITS("Fruits", 350, 2500, "0.05", "0.05"),
    DAIRY("Dairy", 400, 3000, "0.08", "0.08"),
    HOUSEHOLD_ESSENTIALS("Household Essentials", 500, 4000, "0.10", "0.10");

    private final String displayName;
    private final BigDecimal deliveryFee;
    private final BigDecimal minimumOrderValue;
    private final BigDecimal discountRate;
    private final BigDecimal taxRate;

    EUK00361749_Category(String displayName, int deliveryFee, int minimumOrderValue,
                         String discountRate, String taxRate) {
        this.displayName = displayName;
        this.deliveryFee = BigDecimal.valueOf(deliveryFee);
        this.minimumOrderValue = BigDecimal.valueOf(minimumOrderValue);
        this.discountRate = new BigDecimal(discountRate);
        this.taxRate = new BigDecimal(taxRate);
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public BigDecimal getMinimumOrderValue() {
        return minimumOrderValue;
    }

    /** Discount as a fraction (e.g. 0.05) applied when the order exceeds LKR 10,000. */
    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    /** Tax as a fraction (e.g. 0.08). */
    public BigDecimal getTaxRate() {
        return taxRate;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
