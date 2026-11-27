package freshmart;

/**
 * Concrete strategy for the Dairy category (Table 1): delivery fee LKR 400, minimum order
 * value LKR 3000, discount 8% and tax 8%.
 */
public final class EUK00361749_DairyPricing extends EUK00361749_RatePricingStrategy {

    public EUK00361749_DairyPricing() {
        super("400", "3000", "0.08", "0.08");
    }
}
