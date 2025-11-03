import graph.Graph;
import graph.Metrics;
import graph.dagsp.DAGLongestPath;
import graph.dagsp.DAGShortestPath;
import graph.dagsp.SPPath;
import graph.topo.TopologicalSort;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class DAGSPTest {

    @Test
    void shortestPathsFromSourceOnDAG() {
        Graph dag = new Graph(7);
        // Weighted edges (u -> v, w)
        dag.addEdge(0, 1, 2);
        dag.addEdge(0, 2, 4);
        dag.addEdge(1, 3, 7);
        dag.addEdge(1, 4, 1);
        dag.addEdge(2, 4, 3);
        dag.addEdge(3, 5, 1);
        dag.addEdge(4, 5, 5);
        dag.addEdge(2, 6, 2);

        int src = 0;
        DAGShortestPath sssp = new DAGShortestPath(dag, new Metrics());
        DAGShortestPath.Result r = sssp.run(src);

        // Expected shortest distances from 0:
        // 0:0, 1:2, 2:4, 3:9 (0->1->3), 4:3 (0->1->4), 5:8 (0->1->4->5), 6:6 (0->2->6)
        long[] expected = new long[]{0, 2, 4, 9, 3, 8, 6};
        assertArrayEquals(expected, r.dist, "Shortest distances mismatch");
    }

    @Test
    void reconstructsOneShortestPath() {
        Graph dag = new Graph(6);
        dag.addEdge(0, 1, 1);
        dag.addEdge(0, 2, 5);
        dag.addEdge(1, 3, 2);
        dag.addEdge(2, 3, 1);
        dag.addEdge(3, 4, 2);
        dag.addEdge(1, 5, 10);

        int src = 0, dst = 4;
        DAGShortestPath sssp = new DAGShortestPath(dag, new Metrics());
        DAGShortestPath.Result r = sssp.run(src);

        SPPath path = r.reconstructPath(src, dst).orElseThrow();
        assertEquals(Arrays.asList(0, 1, 3, 4), path.nodes(), "Reconstructed shortest path mismatch");
        assertEquals(5L, path.distance(), "Shortest path length mismatch");
    }

    @Test
    void criticalPathLongestDistanceAndReconstruction() {
        Graph dag = new Graph(8);
        dag.addEdge(0, 1, 3);
        dag.addEdge(0, 2, 2);
        dag.addEdge(1, 3, 4);
        dag.addEdge(2, 3, 1);
        dag.addEdge(3, 4, 6);
        dag.addEdge(1, 5, 2);
        dag.addEdge(5, 6, 5);
        dag.addEdge(6, 4, 2);
        dag.addEdge(2, 7, 3);

        // Compute sources = nodes with indegree 0
        Set<Integer> sources = indegreeZero(dag);

        DAGLongestPath lp = new DAGLongestPath(dag, new Metrics());
        DAGLongestPath.Result res = lp.runOptionalSources(sources);

        SPPath critical = res.reconstructAnyLongest().orElseThrow();

        // One critical path: 0 -> 1 -> 3 -> 4 with length 3+4+6 = 13
        assertEquals(Arrays.asList(0, 1, 3, 4), critical.nodes(), "Critical path mismatch");
        assertEquals(13L, critical.distance(), "Critical path length mismatch");

        // Ensure path respects topological order
        assertTrue(isStrictlyIncreasing(critical.nodes(), topoPositions(dag)), "Path must respect DAG order");
    }

    // --- Helpers ---

    private static Set<Integer> indegreeZero(Graph g) {
        int n = g.n();
        int[] indeg = new int[n];
        for (int u = 0; u < n; u++) {
            for (var e : g.neighbors(u)) indeg[e.to]++;
        }
        Set<Integer> s = new HashSet<>();
        for (int i = 0; i < n; i++) if (indeg[i] == 0) s.add(i);
        return s;
    }

    private static Map<Integer, Integer> topoPositions(Graph dag) {
        List<Integer> order = new TopologicalSort(dag, new Metrics()).kahn();
        Map<Integer, Integer> pos = new HashMap<>();
        for (int i = 0; i < order.size(); i++) pos.put(order.get(i), i);
        return pos;
    }

    private static boolean isStrictlyIncreasing(List<Integer> path, Map<Integer, Integer> pos) {
        for (int i = 0; i + 1 < path.size(); i++) {
            if (pos.get(path.get(i)) >= pos.get(path.get(i + 1))) return false;
        }
        return true;
    }
}