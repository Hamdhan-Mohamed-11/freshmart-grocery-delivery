package freshmart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Hand-written merge sort, used to list inventory (by category, then name) and order history (by
 * delivery date, then order ID).
 *
 * <p>Merge sort is O(n log n) in every case and stable, so items that compare equal keep their
 * original order. It is generic: the caller passes a {@link Comparator} describing the ordering.
 */
public final class EUK00361749_Sorter {

    private EUK00361749_Sorter() {
        // Utility class: no objects are needed.
    }

    /**
     * Returns a new sorted list; the input is not modified.
     *
     * <pre>
     * mergeSort(list):
     *   if size &lt;= 1 return list
     *   left  = mergeSort(first half)
     *   right = mergeSort(second half)
     *   return merge(left, right)
     * </pre>
     */
    public static <T> List<T> mergeSort(List<T> items, Comparator<? super T> order) {
        if (items.size() <= 1) {
            return new ArrayList<>(items);
        }
        int middle = items.size() / 2;
        List<T> sortedLeft = mergeSort(items.subList(0, middle), order);
        List<T> sortedRight = mergeSort(items.subList(middle, items.size()), order);
        return merge(sortedLeft, sortedRight, order);
    }

    /** Combines two sorted lists by repeatedly taking the smaller front element. */
    private static <T> List<T> merge(List<T> left, List<T> right, Comparator<? super T> order) {
        List<T> merged = new ArrayList<>(left.size() + right.size());
        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < left.size() && rightIndex < right.size()) {
            // "<=" takes from the left first on ties, which keeps the sort stable.
            if (order.compare(left.get(leftIndex), right.get(rightIndex)) <= 0) {
                merged.add(left.get(leftIndex++));
            } else {
                merged.add(right.get(rightIndex++));
            }
        }
        while (leftIndex < left.size()) {
            merged.add(left.get(leftIndex++));
        }
        while (rightIndex < right.size()) {
            merged.add(right.get(rightIndex++));
        }
        return merged;
    }
}
