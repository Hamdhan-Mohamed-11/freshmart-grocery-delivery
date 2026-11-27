package freshmart;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Shared implementation for strategies that charge a fixed delivery fee and apply percentage
 * discount and tax rates. Each concrete category strategy only supplies its four values from
 * Table 1, so the calculation and rounding rules are written once (abstraction + inheritance).
 */
public abstract class EUK00361749_RatePricingStrategy implements EUK00361749_PricingStrategy {

    private final BigDecimal deliveryFee;
    private final BigDecimal minimumOrderValue;
    private final BigDecimal discountRate;
    private final BigDecimal taxRate;

    /**
     * @param deliveryFee       delivery fee in LKR
     * @param minimumOrderValue minimum order value in LKR
     * @param discountRate      discount as a fraction, e.g. "0.05"
     * @param taxRate           tax as a fraction, e.g. "0.05"
     */
    protected EUK00361749_RatePricingStrategy(String deliveryFee, String minimumOrderValue,
                                              String discountRate, String taxRate) {
        this.deliveryFee = new BigDecimal(deliveryFee).setScale(2);
        this.minimumOrderValue = new BigDecimal(minimumOrderValue).setScale(2);
        this.discountRate = new BigDecimal(discountRate);
        this.taxRate = new BigDecimal(taxRate);
        if (this.deliveryFee.signum() < 0 || this.minimumOrderValue.signum() < 0
                || !isFraction(this.discountRate) || !isFraction(this.taxRate)) {
            throw new IllegalArgumentException("Pricing values must be non-negative and rates between 0 and 1.");
        }
    }

    @Override
    public final BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    @Override
    public final BigDecimal getMinimumOrderValue() {
        return minimumOrderValue;
    }

    public final BigDecimal getDiscountRate() {
        return discountRate;
    }

    public final BigDecimal getTaxRate() {
        return taxRate;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal productValue) {
        return roundMoney(productValue.multiply(discountRate));
    }

    @Override
    public BigDecimal calculateTax(BigDecimal taxableValue) {
        return roundMoney(taxableValue.multiply(taxRate));
    }

    @Override
    public String describe() {
        return String.format("delivery LKR %,.2f | minimum LKR %,.2f | discount %s%% | tax %s%%",
                deliveryFee, minimumOrderValue, percent(discountRate), percent(taxRate));
    }

    /** Money is always rounded half-up to two decimal places (cents). */
    static BigDecimal roundMoney(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean isFraction(BigDecimal rate) {
        return rate.signum() >= 0 && rate.compareTo(BigDecimal.ONE) <= 0;
    }

    private static String percent(BigDecimal rate) {
        return rate.movePointRight(2).stripTrailingZeros().toPlainString();
    }
}
