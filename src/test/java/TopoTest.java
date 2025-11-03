import graph.Graph;
import graph.Metrics;
import graph.topo.TopologicalSort;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Topological Sort tests matched to this repo API:
 * - Graph.addEdge(u,v,w)
 * - TopologicalSort(dag, new Metrics()).kahn()
 * - Validate using Graph.neighbors(u)
 */
public class TopoTest {

    @Test
    void diamondDAGProducesValidOrder() {
        Graph dag = new Graph(6);
        // diamond: 0 -> {1,2} -> {3} -> {4,5}
        dag.addEdge(0, 1, 1);
        dag.addEdge(0, 2, 1);
        dag.addEdge(1, 3, 1);
        dag.addEdge(2, 3, 1);
        dag.addEdge(3, 4, 1);
        dag.addEdge(3, 5, 1);

        List<Integer> order = new TopologicalSort(dag, new Metrics()).kahn();
        assertEquals(6, order.size());
        assertTrue(isTopological(dag, order), "Order must respect all edge directions");
        assertTrue(order.indexOf(0) < order.indexOf(3), "0 must come before 3");
        assertTrue(order.indexOf(1) < order.indexOf(3));
        assertTrue(order.indexOf(2) < order.indexOf(3));
    }

    @Test
    void layeredDAGValidity() {
        Graph dag = new Graph(8);
        // Layers: L0={0}, L1={1,2}, L2={3,4}, L3={5,6,7}
        dag.addEdge(0, 1, 1);
        dag.addEdge(0, 2, 1);
        dag.addEdge(1, 3, 1);
        dag.addEdge(1, 4, 1);
        dag.addEdge(2, 3, 1);
        dag.addEdge(2, 4, 1);
        dag.addEdge(3, 5, 1);
        dag.addEdge(3, 6, 1);
        dag.addEdge(4, 6, 1);
        dag.addEdge(4, 7, 1);

        List<Integer> order = new TopologicalSort(dag, new Metrics()).kahn();
        assertTrue(isTopological(dag, order));
        assertPrecedes(order, 0, List.of(1, 2));
        assertPrecedes(order, List.of(1, 2), List.of(3, 4));
        assertPrecedes(order, List.of(3, 4), List.of(5, 6, 7));
    }

    // --- Helpers ---

    private static boolean isTopological(Graph dag, List<Integer> order) {
        int n = dag.n();
        assertEquals(n, order.size(), "Order size must equal number of vertices");
        int[] pos = new int[n];
        for (int i = 0; i < order.size(); i++) pos[order.get(i)] = i;
        for (int u = 0; u < n; u++) {
            for (var e : dag.neighbors(u)) {
                if (pos[u] >= pos[e.to]) return false;
            }
        }
        return true;
    }

    private static void assertPrecedes(List<Integer> order, int u, List<Integer> vs) {
        for (int v : vs) {
            assertTrue(order.indexOf(u) < order.indexOf(v), "Expected " + u + " before " + v);
        }
    }

    private static void assertPrecedes(List<Integer> order, List<Integer> us, List<Integer> vs) {
        for (int u : us) for (int v : vs) {
            assertTrue(order.indexOf(u) < order.indexOf(v), "Expected " + u + " before " + v);
        }
    }
}