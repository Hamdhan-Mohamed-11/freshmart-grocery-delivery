package freshmart;

/**
 * Observer interface (Observer pattern). The service is the subject: whenever an order is placed,
 * advanced to a new status or cancelled, it calls every registered observer, without knowing what
 * each observer does with the news.
 */
public interface EUK00361749_OrderObserver {

    /**
     * Called after an order's status has changed and the change has been saved.
     *
     * @param order         the order in its new state
     * @param previousState the state before the change, or null for a newly placed order
     */
    void orderStatusChanged(EUK00361749_Order order, EUK00361749_OrderState previousState);
}
