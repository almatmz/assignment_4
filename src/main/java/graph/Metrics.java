package graph;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
/**
 * Metrics utility to track algorithm counters and nanosecond timing.
 * Thread-safe and lightweight; used for empirical validation and profiling.
 *
 * Features:
 * - Increment & accumulate counters
 * - Start & stop timers in nanoseconds
 * - Human-readable output via toString()
 */
public class Metrics {
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> timers = new ConcurrentHashMap<>();

    public void inc(String key) { counters.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet(); }
    public void add(String key, long delta) { counters.computeIfAbsent(key, k -> new AtomicLong()).addAndGet(delta); }

    public long get(String key) { return counters.getOrDefault(key, new AtomicLong(0)).get(); }

    public void timeStart(String label) { timers.put(label + ":start", new AtomicLong(System.nanoTime())); }
    public void timeEnd(String label) {
        AtomicLong start = timers.remove(label + ":start");
        if (start != null) {
            long elapsed = System.nanoTime() - start.get();
            timers.computeIfAbsent(label, k -> new AtomicLong()).addAndGet(elapsed);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Counters:\n");
        counters.forEach((k,v) -> sb.append("  ").append(k).append(": ").append(v.get()).append('\n'));
        sb.append("Timers (ns):\n");
        timers.forEach((k,v) -> sb.append("  ").append(k).append(": ").append(v.get()).append('\n'));
        return sb.toString();
    }
}
