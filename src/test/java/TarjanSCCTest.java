import graph.Graph;
import graph.Metrics;
import graph.scc.TarjanSCC;
import graph.scc.CondensationGraph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TarjanSCC and CondensationGraph using the project's API.
 */
public class TarjanSCCTest {

    @Test
    void testSimpleCycleAndSingletons() {
        Graph g = new Graph(5);
        g.addEdge(0, 1, 1);
        g.addEdge(1, 2, 1);
        g.addEdge(2, 0, 1);
        g.addEdge(3, 4, 1);

        TarjanSCC tarjan = new TarjanSCC(g, new Metrics());
        List<List<Integer>> comps = tarjan.findSCCs();

        assertTrue(comps.size() >= 3, "Expect at least 3 components (cycle + singletons)");
        assertTrue(comps.stream().anyMatch(c -> c.containsAll(List.of(0,1,2))),
                "There must be a component containing nodes 0,1,2");
    }

    @Test
    void testCondensationGraph_hasFewerOrEqualNodes() {
        Graph g = new Graph(4);
        g.addEdge(0, 1, 1);
        g.addEdge(1, 0, 1);
        g.addEdge(1, 2, 1);
        g.addEdge(2, 3, 1);

        TarjanSCC tarjan = new TarjanSCC(g, new Metrics());
        tarjan.findSCCs();
        int[] mapping = tarjan.getComponentMapping();

        CondensationGraph cg = new CondensationGraph(g, mapping);
        Graph dag = cg.build();

        assertTrue(dag.n() <= g.n(), "Condensation graph should have <= original nodes");
        assertTrue(dag.n() < g.n(), "In this example condensation should reduce node count");
    }

    @Test
    void testEmptyGraph_returnsNoSCCs() {
        Graph g = new Graph(0);
        TarjanSCC tarjan = new TarjanSCC(g, new Metrics());
        List<List<Integer>> comps = tarjan.findSCCs();
        assertTrue(comps.isEmpty(), "Empty graph should yield no SCCs");
        int[] mapping = tarjan.getComponentMapping();
        assertEquals(0, mapping.length, "Component mapping for empty graph should be empty array");
    }
}
