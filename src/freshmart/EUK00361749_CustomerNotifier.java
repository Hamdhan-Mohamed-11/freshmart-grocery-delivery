package freshmart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete observer that keeps an in-app message inbox for each customer, so a customer sees
 * "Order ORD0001 changed from Preparing to Out for Delivery" the next time they open their menu.
 *
 * <p>Data structure: a HashMap from customer NIC to a list of messages gives O(1) access to one
 * customer's inbox. Messages are held for the current session only (not saved to disk).
 */
public final class EUK00361749_CustomerNotifier implements EUK00361749_OrderObserver {

    private final Map<String, List<String>> messagesByCustomer = new HashMap<>();

    @Override
    public void orderStatusChanged(EUK00361749_Order order, EUK00361749_OrderState previousState) {
        String message = previousState == null
                ? "Order " + order.orderId() + " placed for delivery on " + order.deliveryDate() + " (" + order.timeSlot() + ")."
                : "Order " + order.orderId() + " changed from " + previousState + " to " + order.state() + ".";
        messagesByCustomer.computeIfAbsent(order.customerId(), key -> new ArrayList<>()).add(message);
    }

    /** Returns and clears the unread messages for one customer. */
    public List<String> collectMessages(String customerId) {
        List<String> messages = messagesByCustomer.remove(customerId);
        return messages == null ? List.of() : messages;
    }

    /** @return how many unread messages a customer has. */
    public int countMessages(String customerId) {
        return messagesByCustomer.getOrDefault(customerId, List.of()).size();
    }
}
