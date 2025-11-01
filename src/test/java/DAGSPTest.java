import graph.Graph;
import graph.Metrics;
import graph.dagsp.DAGShortestPath;
import graph.dagsp.DAGLongestPath;
import graph.dagsp.SPPath;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DAG shortest/longest path algorithms using the project's API.
 */
public class DAGSPTest {

    @Test
    void testShortestPath_simpleGraph() {
        Graph g = new Graph(5);
        g.addEdge(0, 1, 2);
        g.addEdge(0, 2, 4);
        g.addEdge(1, 3, 7);
        g.addEdge(2, 3, 1);
        g.addEdge(3, 4, 3);

        DAGShortestPath sp = new DAGShortestPath(g, new Metrics());
        DAGShortestPath.Result res = sp.run(0);

        assertEquals(0, res.dist[0]);
        assertEquals(2, res.dist[1]);
        assertEquals(4, res.dist[2]);
        assertEquals(5, res.dist[3]);
        assertEquals(8, res.dist[4]);


        Optional<SPPath> pathOpt = res.reconstructPath(0, 4);
        assertTrue(pathOpt.isPresent(), "Path 0->4 should be reconstructable");
        SPPath path = pathOpt.get();
        List<Integer> expected = List.of(0, 2, 3, 4);
        assertEquals(expected, path.nodes());
        assertEquals(8, path.distance());
    }

    @Test
    void testLongestPath_simpleGraph() {
        Graph g = new Graph(5);
        g.addEdge(0, 1, 5);
        g.addEdge(1, 2, 10);
        g.addEdge(0, 3, 1);
        g.addEdge(3, 4, 2);
        g.addEdge(2, 4, 3);

        DAGLongestPath lp = new DAGLongestPath(g, new Metrics());
        DAGLongestPath.Result lres = lp.runOptionalSources(Set.of(0));

        Optional<SPPath> longest = lres.reconstructAnyLongest();
        assertTrue(longest.isPresent(), "Longest path should be found");
        SPPath p = longest.get();
        assertEquals(18, p.distance());
        assertEquals(List.of(0, 1, 2, 4), p.nodes());
    }

    @Test
    void testShortestPath_disconnectedNodes() {
        Graph g = new Graph(4);
        g.addEdge(0, 1, 1);

        DAGShortestPath sp = new DAGShortestPath(g, new Metrics());
        DAGShortestPath.Result res = sp.run(0);

        final long INF = Long.MAX_VALUE / 4;
        assertEquals(0, res.dist[0]);
        assertEquals(1, res.dist[1]);
        assertEquals(INF, res.dist[2], "Node 2 should be unreachable");
        assertEquals(INF, res.dist[3], "Node 3 should be unreachable");
    }
}
