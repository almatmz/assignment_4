package graph.dagsp;

import graph.Graph;
import graph.Metrics;
import graph.TopologicalSort;

import java.util.*;

public class DAGShortestPath {
    private final Graph dag;
    private final Metrics metrics;

    public DAGShortestPath(Graph dag, Metrics metrics) {
        this.dag = dag;
        this.metrics = metrics;
    }


    public Result run(int source) {
        metrics.timeStart("dagsp_total");
        TopologicalSort topo = new TopologicalSort(dag, metrics);
        List<Integer> order = topo.kahn();
        int n = dag.n();
        final long INF = Long.MAX_VALUE / 4;
        long[] dist = new long[n];
        Arrays.fill(dist, INF);
        int[] pre = new int[n];
        Arrays.fill(pre, -1);

        dist[source] = 0;
        int[] pos = new int[n];
        for (int i = 0; i < n; i++) pos[order.get(i)] = i;

        for (int u : order) {
            if (dist[u] == INF) continue;
            for (Graph.Edge e : dag.neighbors(u)) {
                int v = e.to; long w = e.weight;
                metrics.inc("relaxations");
                if (dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;
                    pre[v] = u;
                }
            }
        }
        metrics.timeEnd("dagsp_total");
        return new Result(dist, pre);
    }

    public static class Result {
        public final long[] dist;
        public final int[] pre;
        public Result(long[] dist, int[] pre) { this.dist = dist; this.pre = pre; }
        public Optional<SPPath> reconstructPath(int src, int tgt) {
            if (dist[tgt] == Long.MAX_VALUE/4) return Optional.empty();
            LinkedList<Integer> ls = new LinkedList<>();
            int cur = tgt;
            while (cur != -1) { ls.addFirst(cur); if (cur == src) break; cur = pre[cur]; }
            return Optional.of(new SPPath(ls, dist[tgt]));
        }
    }
}
