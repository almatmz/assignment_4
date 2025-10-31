package graph.dagsp;

import graph.Graph;
import graph.Metrics;
import graph.TopologicalSort;

import java.util.*;

public class DAGLongestPath {
    private final Graph dag;
    private final Metrics metrics;

    public DAGLongestPath(Graph dag, Metrics metrics) {
        this.dag = dag;
        this.metrics = metrics;
    }

    public Result runOptionalSources(Set<Integer> sources) {
        metrics.timeStart("daglong_total");
        TopologicalSort topo = new TopologicalSort(dag, metrics);
        List<Integer> order = topo.kahn();
        int n = dag.n();
        final long NEG_INF = Long.MIN_VALUE / 4;
        long[] dist = new long[n];
        Arrays.fill(dist, NEG_INF);
        int[] pre = new int[n];
        Arrays.fill(pre, -1);


        if (sources == null || sources.isEmpty()) {
            for (int i = 0; i < n; i++) if (dag.neighbors(i).size() == 0) dist[i] = 0;
        } else {
            for (int s : sources) dist[s] = 0;
        }

        for (int u : order) {
            if (dist[u] == NEG_INF) continue;
            for (Graph.Edge e : dag.neighbors(u)) {
                int v = e.to; long w = e.weight;
                metrics.inc("long_relaxations");
                if (dist[u] + w > dist[v]) {
                    dist[v] = dist[u] + w;
                    pre[v] = u;
                }
            }
        }
        metrics.timeEnd("daglong_total");
        return new Result(dist, pre);
    }

    public static class Result {
        public final long[] dist;
        public final int[] pre;
        public Result(long[] dist, int[] pre) { this.dist = dist; this.pre = pre; }

        public Optional<SPPath> reconstructAnyLongest() {
            int n = dist.length;
            long best = Long.MIN_VALUE;
            int bestIdx = -1;
            for (int i = 0; i < n; i++) {
                if (dist[i] > best) { best = dist[i]; bestIdx = i; }
            }
            if (bestIdx == -1 || best == Long.MIN_VALUE/4) return Optional.empty();
            LinkedList<Integer> ls = new LinkedList<>();
            int cur = bestIdx;
            while (cur != -1) { ls.addFirst(cur); cur = pre[cur]; }
            return Optional.of(new SPPath(ls, best));
        }
    }
}
