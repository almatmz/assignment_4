package graph.topo;

import graph.Graph;
import graph.Metrics;

import java.util.*;
/**
 * Performs topological sorting on a directed acyclic graph (DAG).
 * Uses Kahn's Algorithm (BFS + indegree tracking).
 */
public class TopologicalSort {
    private final Graph dag;
    private final Metrics metrics;

    public TopologicalSort(Graph dag, Metrics metrics) {
        this.dag = dag;
        this.metrics = metrics;
    }
    /**
     * Computes a topological order using Kahn's algorithm.
     * @return list of nodes in valid topological order
     */
    public List<Integer> kahn() {
        metrics.timeStart("kahn_total");
        int n = dag.n();
        int[] indeg = new int[n];

        // Compute indegree for each vertex
        for (int u = 0; u < n; u++) {
            for (var e : dag.neighbors(u)) indeg[e.to]++;
        }

        ArrayDeque<Integer> q = new ArrayDeque<>();
        // enqueue all zero-indegree nodes
        for (int i = 0; i < n; i++) {
            if (indeg[i] == 0) { q.add(i); metrics.inc("kahnPushes"); }
        }

        List<Integer> order = new ArrayList<>(n);
        // BFS pop + edge removal
        while (!q.isEmpty()) {
            int u = q.removeFirst(); metrics.inc("kahnPops");
            order.add(u);
            for (var e : dag.neighbors(u)) {
                indeg[e.to]--;
                if (indeg[e.to] == 0) { q.add(e.to); metrics.inc("kahnPushes"); }
            }
        }
        metrics.timeEnd("kahn_total");
        if (order.size() != n) return Collections.emptyList();
        return order;
    }
}
