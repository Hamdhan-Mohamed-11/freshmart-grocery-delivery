package freshmart;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Second concrete observer: an activity log of every order status change, shown to the
 * administrator. It shows that one subject can serve several independent observers; neither the
 * service nor the customer notifier needed to change when the log was added.
 */
public final class EUK00361749_OrderActivityLog implements EUK00361749_OrderObserver {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Clock clock;
    private final List<String> entries = new ArrayList<>();

    public EUK00361749_OrderActivityLog(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void orderStatusChanged(EUK00361749_Order order, EUK00361749_OrderState previousState) {
        entries.add(String.format("%s | %-8s | NIC %-12s | %-16s -> %s",
                LocalDateTime.now(clock).format(TIME_FORMAT), order.orderId(), order.customerId(),
                previousState == null ? "(new order)" : previousState, order.state()));
    }

    /** @return every entry recorded in this session, oldest first (read-only). */
    public List<String> getEntries() {
        return List.copyOf(entries);
    }
}
