package freshmart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete observer that keeps an in-app message inbox for each customer, so a customer sees
 * "Your order ORD0001 is now Out for Delivery" the next time they log in.
 *
 * <p>Data structure: a HashMap from customer NIC to a list of messages gives O(1) access to one
 * customer's inbox. Messages are held for the current session only (not saved to disk).
 */
public final class EUK00361749_CustomerNotifier implements EUK00361749_OrderObserver {

    private final Map<String, List<String>> messagesByCustomer = new HashMap<>();

    @Override
    public void orderStatusChanged(EUK00361749_Order order, EUK00361749_OrderStatus previousStatus) {
        String message = previousStatus == null
                ? "Order " + order.orderId() + " placed for delivery on " + order.deliveryDate() + " (" + order.timeSlot() + ")."
                : "Order " + order.orderId() + " changed from " + previousStatus + " to " + order.status() + ".";
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
