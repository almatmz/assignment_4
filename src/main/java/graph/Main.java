package graph;

import graph.scc.TarjanSCC;
import graph.scc.CondensationGraph;
import graph.dagsp.DAGShortestPath;
import graph.dagsp.DAGLongestPath;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import graph.topo.TopologicalSort;

public class Main {

    private static final String DATA_DIR = "data";
    private static final String OUTPUT_DIR = "output";

    public static void main(String[] args) throws Exception {
        new File(OUTPUT_DIR).mkdirs();

        List<Path> files = Files.list(Paths.get(DATA_DIR))
                .filter(p -> p.toString().endsWith(".json"))
                .sorted()
                .collect(Collectors.toList());

        List<Object> smallResults = new ArrayList<>();
        List<Object> mediumResults = new ArrayList<>();
        List<Object> largeResults = new ArrayList<>();

        for (Path p : files) {
            String fname = p.getFileName().toString();
            System.out.println("\nProcessing: " + fname);

            Graph g = JsonGraphLoader.load(p.toString());
            int n = g.n();
            int m = countEdges(g);
            double density = (double) m / Math.max(1, n);

            // --- SCC ---
            Metrics mSCC = new Metrics();
            long t0 = System.nanoTime();
            TarjanSCC tarjan = new TarjanSCC(g, mSCC);
            List<List<Integer>> sccs = tarjan.findSCCs();
            long sccNs = System.nanoTime() - t0;

            int[] comp = tarjan.getComponentMapping();
            CondensationGraph cg = new CondensationGraph(g, comp);
            Graph dag = cg.build();

            // --- Topological Sort ---
            Metrics mTopo = new Metrics();
            long t1 = System.nanoTime();
            TopologicalSort topo = new TopologicalSort(dag, mTopo);
            List<Integer> topoOrder = topo.kahn();
            long topoNs = System.nanoTime() - t1;

            // --- Shortest Path ---
            Metrics mSP = new Metrics();
            DAGShortestPath spAlg = new DAGShortestPath(dag, mSP);
            int src = (g.getSource() >= 0) ? comp[g.getSource()] : 0;

            long t2 = System.nanoTime();
            var sp = spAlg.run(src);
            long spNs = System.nanoTime() - t2;

            long INF = Long.MAX_VALUE / 4;
            int reachable = 0;
            double sum = 0;
            long maxDist = Long.MIN_VALUE;
            int bestNode = -1;

            for (int i = 0; i < sp.dist.length; i++) {
                long d = sp.dist[i];
                if (d != INF) {
                    reachable++;
                    sum += d;
                    if (d > maxDist) {
                        maxDist = d;
                        bestNode = i;
                    }
                }
            }

            long bestDistance = 0;
            List<Integer> bestPath = List.of();
            if (bestNode >= 0) {
                var opt = sp.reconstructPath(src, bestNode);
                if (opt.isPresent()) {
                    bestDistance = opt.get().distance();
                    bestPath = opt.get().nodes();
                }
            }

            // --- Longest Path ---
            Metrics mLP = new Metrics();
            DAGLongestPath lpAlg = new DAGLongestPath(dag, mLP);

            long t3 = System.nanoTime();
            var lpRes = lpAlg.runOptionalSources(Set.of(src));
            long lpNs = System.nanoTime() - t3;

            long crit = 0;
            List<Integer> critPath = List.of();
            var opt2 = lpRes.reconstructAnyLongest();
            if (opt2.isPresent()) {
                crit = opt2.get().distance();
                critPath = opt2.get().nodes();
            }

            // --- Build JSON result ---
            Map<String, Object> json = new LinkedHashMap<>();

            json.put("file", fname);
            json.put("nodes", n);
            json.put("edges", m);
            json.put("density", density);

            json.put("SCC", Map.of(
                    "components", sccs,
                    "component_count", sccs.size(),
                    "avg_component_size", sccs.stream().mapToInt(List::size).average().orElse(0),
                    "condensed_nodes", dag.n(),
                    "dfs_visits", mSCC.get("dfsVisits"),
                    "dfs_edges", mSCC.get("dfsEdges"),
                    "time_ns", sccNs
            ));

            json.put("Topological_Order", Map.of(
                    "order", topoOrder,
                    "valid_dag", topoOrder.size() == dag.n(),
                    "push_ops", mTopo.get("kahnPushes"),
                    "pop_ops", mTopo.get("kahnPops"),
                    "time_ns", topoNs
            ));

            json.put("Shortest_Path", Map.of(
                    "source_component", src,
                    "reachable_nodes", reachable,
                    "avg_distance", reachable == 0 ? 0 : sum / reachable,
                    "best_distance", bestDistance,
                    "best_path", bestPath,
                    "relax_ops", mSP.get("relaxations"),
                    "time_ns", spNs
            ));

            json.put("Longest_Path", Map.of(
                    "critical_length", crit,
                    "critical_path", critPath,
                    "relax_ops", mLP.get("long_relaxations"),
                    "time_ns", lpNs
            ));

            String f = fname.toLowerCase();
            if (f.contains("small")) smallResults.add(json);
            else if (f.contains("medium")) mediumResults.add(json);
            else largeResults.add(json);

            System.out.println("Done: " + fname);
        }

        ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
        writer.writeValue(new File(OUTPUT_DIR + "/results_small.json"), smallResults);
        writer.writeValue(new File(OUTPUT_DIR + "/results_medium.json"), mediumResults);
        writer.writeValue(new File(OUTPUT_DIR + "/results_large.json"), largeResults);

        System.out.println("\n All results saved in /output/ .");
    }

    private static int countEdges(Graph g) {
        int c = 0;
        for (int i = 0; i < g.n(); i++) c += g.neighbors(i).size();
        return c;
    }
}
