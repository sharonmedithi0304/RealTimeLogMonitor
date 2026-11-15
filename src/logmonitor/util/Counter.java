package logmonitor.util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Generic class: counts how many times each key of type T occurs.
 * T extends Comparable<T> (a bounded type parameter) so the keys can be sorted.
 * The same class is reused for log levels (Counter<LogLevel>) and for text
 * (Counter<String>), which is the main advantage of generics.
 * Not thread-safe by itself; LogAnalyzer synchronizes access.
 */
public class Counter<T extends Comparable<T>> {

    private final HashMap<T, Integer> counts = new HashMap<>();

    public void increment(T key) {
        counts.merge(key, 1, Integer::sum);
    }

    public int get(T key) {
        return counts.getOrDefault(key, 0);
    }

    public int total() {
        int sum = 0;
        for (int value : counts.values()) {
            sum += value;
        }
        return sum;
    }

    public int distinct() {
        return counts.size();
    }

    public void clear() {
        counts.clear();
    }

    /** TreeMap keeps the keys in sorted order (alphabetical for Strings). */
    public TreeMap<T, Integer> sortedByKey() {
        return new TreeMap<>(counts);
    }

    /** The n most frequent keys, highest count first (ties broken by key). */
    public List<Map.Entry<T, Integer>> topN(int n) {
        List<Map.Entry<T, Integer>> list = new ArrayList<>();
        for (Map.Entry<T, Integer> e : counts.entrySet()) {
            list.add(new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue()));
        }
        // Comparator written as a lambda expression
        list.sort((a, b) -> {
            int byCount = Integer.compare(b.getValue(), a.getValue());
            return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
        });
        return new ArrayList<>(list.subList(0, Math.min(n, list.size())));
    }
}
