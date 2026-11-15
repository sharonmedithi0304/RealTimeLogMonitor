package logmonitor.util;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/** Small helper methods written as generic methods. */
public final class ListUtils {

    private ListUtils() {
        // utility class: no objects needed
    }

    /**
     * Generic method: returns the last n elements of any list, newest (last) first.
     * A ListIterator is used to walk the list backwards.
     */
    public static <T> List<T> lastN(List<T> list, int n) {
        List<T> result = new ArrayList<>();
        ListIterator<T> iterator = list.listIterator(list.size());
        while (iterator.hasPrevious() && result.size() < n) {
            result.add(iterator.previous());
        }
        return result;
    }
}
