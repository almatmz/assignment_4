import graph.Graph;
import graph.Metrics;
import graph.scc.CondensationGraph;
import graph.scc.TarjanSCC;
import graph.topo.TopologicalSort;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SCC tests matched to this repo API:
 * - Graph.addEdge(u,v,w)
 * - Graph.n(), Graph.neighbors(u)
 * - TarjanSCC(Graph, Metrics) and findSCCs()
 * - Condensation via CondensationGraph
 * - Topological order via TopologicalSort(Graph, Metrics).kahn()
 */
public class TarjanSCCTest {

    @Test
    void findsMultipleSCCsWithBridgingEdges() {
        Graph g = new Graph(8);
        // SCC1: {0,1,2}
        g.addEdge(0, 1, 1);
        g.addEdge(1, 2, 1);
        g.addEdge(2, 0, 1);
        // SCC2: {3,4}
        g.addEdge(3, 4, 1);
        g.addEdge(4, 3, 1);
        // SCC4: {6,7}
        g.addEdge(6, 7, 1);
        g.addEdge(7, 6, 1);
        // Bridges: (SCC1)->(SCC2)->5->(SCC4)
        g.addEdge(2, 3, 1);
        g.addEdge(4, 5, 1);
        g.addEdge(5, 6, 1);

        TarjanSCC tarjan = new TarjanSCC(g, new Metrics());
        List<List<Integer>> sccs = tarjan.findSCCs();

        Set<Set<Integer>> actual = sccs.stream().map(HashSet::new).collect(Collectors.toSet());
        Set<Set<Integer>> expected = new HashSet<>();
        expected.add(setOf(0, 1, 2));
        expected.add(setOf(3, 4));
        expected.add(setOf(5));
        expected.add(setOf(6, 7));

        assertEquals(expected, actual, "SCC partition mismatch");
    }

    @Test
    void acyclicGraphProducesOnlySingletonSCCs() {
        Graph dag = new Graph(7);
        dag.addEdge(0, 1, 1);
        dag.addEdge(0, 2, 1);
        dag.addEdge(1, 3, 1);
        dag.addEdge(2, 3, 1);
        dag.addEdge(3, 4, 1);
        dag.addEdge(4, 5, 1);
        dag.addEdge(2, 6, 1);

        TarjanSCC tarjan = new TarjanSCC(dag, new Metrics());
        List<List<Integer>> sccs = tarjan.findSCCs();

        assertEquals(7, sccs.size(), "Every vertex should be its own SCC in a DAG");
        for (List<Integer> comp : sccs) {
            assertEquals(1, comp.size(), "All components must be singletons");
        }
    }

    @Test
    void handlesSelfLoopsAsSingleVertexSCCs() {
        Graph g = new Graph(4);
        g.addEdge(0, 0, 1); // self-loop => SCC {0}
        g.addEdge(1, 2, 1);
        g.addEdge(2, 3, 1);

        TarjanSCC tarjan = new TarjanSCC(g, new Metrics());
        List<List<Integer>> sccs = tarjan.findSCCs();

        Optional<List<Integer>> comp0 = sccs.stream().filter(c -> c.contains(0)).findFirst();
        assertTrue(comp0.isPresent(), "Vertex 0 must appear in some SCC");
        assertEquals(1, comp0.get().size(), "Self-loop vertex should form an SCC of size 1");
    }

    @Test
    void condensationIsADAGAndTopoSortable() {
        Graph g = new Graph(6);
        // SCC A: {0,1}
        g.addEdge(0, 1, 1);
        g.addEdge(1, 0, 1);
        // SCC C: {3,4,5}
        g.addEdge(3, 4, 1);
        g.addEdge(4, 5, 1);
        g.addEdge(5, 3, 1);
        // Edges between SCCs: A -> 2 -> C
        g.addEdge(1, 2, 1);
        g.addEdge(2, 3, 1);

        TarjanSCC tarjan = new TarjanSCC(g, new Metrics());
        tarjan.findSCCs();

        Graph condensation = new CondensationGraph(g, tarjan.getComponentMapping()).build();
        List<Integer> order = new TopologicalSort(condensation, new Metrics()).kahn();

        assertEquals(condensation.n(), order.size(), "Topo order must include all condensed nodes");
        assertTrue(isTopological(condensation, order), "Condensation must be a DAG (valid topological order)");
    }

    // --- Helpers ---

    private static boolean isTopological(Graph dag, List<Integer> order) {
        int n = dag.n();
        int[] pos = new int[n];
        for (int i = 0; i < order.size(); i++) pos[order.get(i)] = i;
        for (int u = 0; u < n; u++) {
            for (var e : dag.neighbors(u)) {
                if (pos[u] >= pos[e.to]) return false;
            }
        }
        return true;
    }

    private static Set<Integer> setOf(Integer... vals) {
        return new HashSet<>(Arrays.asList(vals));
    }
}