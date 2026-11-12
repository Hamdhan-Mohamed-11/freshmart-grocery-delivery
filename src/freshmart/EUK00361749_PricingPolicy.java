package freshmart;

import java.util.List;

/**
 * Pricing rule interface (Strategy design pattern). The service is given a policy object and calls
 * {@link #calculate(List)} without knowing how prices are worked out, so a different pricing
 * scheme (e.g. a seasonal promotion) can be plugged in without changing the service.
 */
public interface EUK00361749_PricingPolicy {

    /**
     * Prices a basket.
     *
     * @param lines one or more order lines
     * @return the invoice amounts
     * @throws IllegalArgumentException if the basket is empty or below the minimum order value
     */
    EUK00361749_Invoice calculate(List<EUK00361749_OrderLine> lines);
}
