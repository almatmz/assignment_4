package graph;

import java.util.*;

public class TopologicalSort {
    private final Graph dag;
    private final Metrics metrics;

    public TopologicalSort(Graph dag, Metrics metrics) {
        this.dag = dag;
        this.metrics = metrics;
    }

    public List<Integer> kahn() {
        metrics.timeStart("kahn_total");
        int n = dag.n();
        int[] indeg = new int[n];
        for (int u = 0; u < n; u++) {
            for (var e : dag.neighbors(u)) indeg[e.to]++;
        }

        ArrayDeque<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            if (indeg[i] == 0) { q.add(i); metrics.inc("kahnPushes"); }
        }

        List<Integer> order = new ArrayList<>(n);
        while (!q.isEmpty()) {
            int u = q.removeFirst(); metrics.inc("kahnPops");
            order.add(u);
            for (var e : dag.neighbors(u)) {
                indeg[e.to]--;
                if (indeg[e.to] == 0) { q.add(e.to); metrics.inc("kahnPushes"); }
            }
        }
        metrics.timeEnd("kahn_total");
        if (order.size() != n) return Collections.emptyList(); // not a DAG (cycle)
        return order;
    }
}
