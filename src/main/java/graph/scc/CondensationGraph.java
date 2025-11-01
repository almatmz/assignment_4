package graph.scc;

import graph.Graph;
import java.util.*;
/**
 * Builds the condensation graph (SCC DAG) from component mapping produced by TarjanSCC.
 * Each SCC becomes a single vertex; edges are compressed and minimum edge weight kept.
 */
public class CondensationGraph {
    private final Graph original;
    private final int[] compOf;
    private final int compCount;
    // For each component, store best (minimum-weight) outgoing edges to other components
    private final List<Map<Integer, Long>> compAdj;

    public CondensationGraph(Graph original, int[] compOf) {
        this.original = original;
        this.compOf = compOf;
        this.compCount = Arrays.stream(compOf).max().orElse(-1) + 1;
        this.compAdj = new ArrayList<>(compCount);
        for (int i = 0; i < compCount; i++) compAdj.add(new HashMap<>());
    }

    /**
     * Builds and returns a DAG where each node is an SCC and edges represent compressed SCC edges.
     *
     * @return Directed Acyclic Graph of SCCs
     */
    public Graph build() {
        Graph dag = new Graph(compCount);
        for (int u = 0; u < original.n(); u++) {
            int cu = compOf[u];
            for (Graph.Edge e : original.neighbors(u)) {
                int v = e.to;
                int cv = compOf[v];
                if (cu == cv) continue;
                Map<Integer, Long> map = compAdj.get(cu);
                long w = e.weight;
                long prev = map.getOrDefault(cv, Long.MAX_VALUE);
                if (w < prev) map.put(cv, w);
            }
        }
        for (int cu = 0; cu < compCount; cu++) {
            Map<Integer, Long> map = compAdj.get(cu);
            for (var entry : map.entrySet()) {
                dag.addEdge(cu, entry.getKey(), entry.getValue());
            }
        }
        return dag;
    }
}
