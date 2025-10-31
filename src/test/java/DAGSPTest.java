
import graph.Graph;
import graph.Metrics;
import graph.dagsp.DAGLongestPath;
import graph.dagsp.DAGShortestPath;
import graph.dagsp.SPPath;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class DAGSPTest {

    @Test
    void testShortestPath() {
        Graph g = new Graph(5);
        g.addEdge(0, 1, 2);
        g.addEdge(0, 2, 4);
        g.addEdge(1, 3, 7);
        g.addEdge(2, 3, 1);
        g.addEdge(3, 4, 3);

        DAGShortestPath sp = new DAGShortestPath(g, new Metrics());
        DAGShortestPath.Result res = sp.run(0);

        assertEquals(0, res.dist[0]);
        assertTrue(res.dist[3] <= 5);
        Optional<SPPath> path = res.reconstructPath(0, 4);
        assertTrue(path.isPresent());
        System.out.println(path.get());
    }

    @Test
    void testLongestPath() {
        Graph g = new Graph(5);
        g.addEdge(0, 1, 5);
        g.addEdge(1, 2, 10);
        g.addEdge(0, 3, 1);
        g.addEdge(3, 4, 2);
        g.addEdge(2, 4, 3);

        DAGLongestPath lp = new DAGLongestPath(g, new Metrics());
        DAGLongestPath.Result res = lp.runOptionalSources(Set.of(0));

        Optional<SPPath> longest = res.reconstructAnyLongest();
        assertTrue(longest.isPresent());
        System.out.println("Longest: " + longest.get());
        assertTrue(longest.get().distance() > 0);
    }
}
