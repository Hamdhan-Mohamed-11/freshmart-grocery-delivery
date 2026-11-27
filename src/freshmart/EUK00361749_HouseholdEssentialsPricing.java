package freshmart;

/**
 * Concrete strategy for the Household Essentials category (Table 1): delivery fee LKR 500, minimum order
 * value LKR 4000, discount 10% and tax 10%.
 */
public final class EUK00361749_HouseholdEssentialsPricing extends EUK00361749_RatePricingStrategy {

    public EUK00361749_HouseholdEssentialsPricing() {
        super("500", "4000", "0.10", "0.10");
    }
}
