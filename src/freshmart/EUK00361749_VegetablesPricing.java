package freshmart;

/**
 * Concrete strategy for the Vegetables category (Table 1): delivery fee LKR 300, minimum order
 * value LKR 2000, discount 5% and tax 5%.
 */
public final class EUK00361749_VegetablesPricing extends EUK00361749_RatePricingStrategy {

    public EUK00361749_VegetablesPricing() {
        super("300", "2000", "0.05", "0.05");
    }
}
