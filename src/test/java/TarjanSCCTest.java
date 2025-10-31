
import graph.Graph;
import graph.Metrics;
import graph.scc.CondensationGraph;
import graph.scc.TarjanSCC;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class TarjanSCCTest {

    @Test
    void testSimpleSCCs() {
        Graph g = new Graph(5);
        g.addEdge(0, 1, 1);
        g.addEdge(1, 2, 1);
        g.addEdge(2, 0, 1);
        g.addEdge(3, 4, 1);

        TarjanSCC tarjan = new TarjanSCC(g, new Metrics());
        List<List<Integer>> sccs = tarjan.findSCCs();

        assertEquals(3, sccs.size());

        boolean foundCycle = sccs.stream().anyMatch(comp -> comp.containsAll(Arrays.asList(0,1,2)));
        assertTrue(foundCycle, "Should detect 0-1-2 as one SCC");

        int[] mapping = tarjan.getComponentMapping();
        assertEquals(mapping[0], mapping[1]);
        assertEquals(mapping[1], mapping[2]);
        assertNotEquals(mapping[3], mapping[4]);
    }

    @Test
    void testCondensationGraph() {
        Graph g = new Graph(4);
        g.addEdge(0, 1, 1);
        g.addEdge(1, 0, 1);
        g.addEdge(1, 2, 1);
        g.addEdge(2, 3, 1);

        TarjanSCC tarjan = new TarjanSCC(g, new Metrics());
        tarjan.findSCCs();
        int[] comp = tarjan.getComponentMapping();

        CondensationGraph cg = new CondensationGraph(g, comp);
        Graph dag = cg.build();
        assertTrue(dag.n() < g.n(), "Condensation graph should have fewer nodes");
    }
}
