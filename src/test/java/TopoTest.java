import graph.Graph;
import graph.Metrics;
import graph.topo.TopologicalSort;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class TopoTest {

    @Test
    void testKahnSort() {
        Graph g = new Graph(4);
        g.addEdge(0, 1, 1);
        g.addEdge(1, 2, 1);
        g.addEdge(0, 3, 1);

        TopologicalSort topo = new TopologicalSort(g, new Metrics());
        List<Integer> order = topo.kahn();

        assertEquals(4, order.size());
        assertTrue(order.indexOf(0) < order.indexOf(1));
        assertTrue(order.indexOf(1) < order.indexOf(2));
    }

    @Test
    void testCycleReturnsEmpty() {
        Graph g = new Graph(3);
        g.addEdge(0, 1, 1);
        g.addEdge(1, 2, 1);
        g.addEdge(2, 0, 1);

        TopologicalSort topo = new TopologicalSort(g, new Metrics());
        List<Integer> order = topo.kahn();

        assertTrue(order.isEmpty(), "Cycle should produce empty topo order");
    }

    //  Edge case: empty graph
    @Test
    void testEmptyGraph() {
        Graph g = new Graph(0);
        TopologicalSort topo = new TopologicalSort(g, new Metrics());
        List<Integer> order = topo.kahn();
        assertTrue(order.isEmpty(), "Empty graph should return an empty topological order");
    }
}
