package graph.scc;

import graph.Graph;
import graph.Metrics;

import java.util.*;

/**
 * Tarjan's Strongly Connected Components algorithm implementation.
 *
 * <p>This class computes SCCs in a directed graph in O(V + E) time.
 * It uses DFS traversal, low-link values, and a stack to detect SCC roots.
 * Each vertex is pushed once and popped once. Metrics are collected for analysis.</p>
 */
public class TarjanSCC {
    private final Graph g;
    private final Metrics metrics;

    private int index = 0;
    private final int[] indices;
    private final int[] low;
    private final boolean[] onStack;
    private final Deque<Integer> stack = new ArrayDeque<>();
    private final List<List<Integer>> components = new ArrayList<>();
    private final int[] compOf;

    public TarjanSCC(Graph g, Metrics metrics) {
        this.g = g;
        this.metrics = metrics;
        this.indices = new int[g.n()];
        Arrays.fill(indices, -1);
        this.low = new int[g.n()];
        this.onStack = new boolean[g.n()];
        this.compOf = new int[g.n()];
        Arrays.fill(compOf, -1);
    }
    /**
     * Computes all strongly connected components.
     *
     * @return List of components, each as a list of vertices.
     */
    public List<List<Integer>> findSCCs() {
        metrics.timeStart("scc_total");
        for (int v = 0; v < g.n(); v++) {
            if (indices[v] == -1) strongConnect(v);
        }
        metrics.timeEnd("scc_total");
        return components;
    }

    /**
     * Depth-first search for SCC; core Tarjan logic.
     */

    private void strongConnect(int v) {
        indices[v] = index;
        low[v] = index;
        index++;
        stack.push(v);
        onStack[v] = true;
        metrics.inc("dfsVisits");

        // Explore neighbors
        for (Graph.Edge e : g.neighbors(v)) {
            int w = e.to;
            metrics.inc("dfsEdges");
            if (indices[w] == -1) {
                strongConnect(w);
                low[v] = Math.min(low[v], low[w]);
            } else if (onStack[w]) {
                low[v] = Math.min(low[v], indices[w]);
            }
        }

        // If v is root of an SCC
        if (low[v] == indices[v]) {
            List<Integer> comp = new ArrayList<>();
            while (true) {
                int w = stack.pop();
                onStack[w] = false;
                compOf[w] = components.size();
                comp.add(w);
                if (w == v) break;
            }
            components.add(comp);
        }
    }

    public int[] getComponentMapping() {
        return Arrays.copyOf(compOf, compOf.length);
    }
}
