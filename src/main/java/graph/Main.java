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

public class Main {

    private static final String DATA_DIR = "data";
    private static final String OUTPUT_DIR = "output";

    public static void main(String[] args) throws Exception {
        new File(OUTPUT_DIR).mkdirs();

        List<Path> files = Files.list(Paths.get(DATA_DIR))
                .filter(p -> p.toString().endsWith(".json"))
                .sorted()
                .collect(Collectors.toList());

        List<Map<String, Object>> smallResults = new ArrayList<>();
        List<Map<String, Object>> mediumResults = new ArrayList<>();
        List<Map<String, Object>> largeResults = new ArrayList<>();

        for (Path p : files) {
            String fname = p.getFileName().toString();
            System.out.println("Processing: " + fname);

            Graph g = JsonGraphLoader.load(p.toString());

            int n = g.n();
            int m = countEdges(g);
            double density = (double) m / Math.max(1, n);

            // ===== SCC =====
            Metrics mSCC = new Metrics();
            long t0 = System.nanoTime();
            TarjanSCC t = new TarjanSCC(g, mSCC);
            List<List<Integer>> sccs = t.findSCCs();
            long sccTime = (System.nanoTime() - t0) / 1_000_000;

            int[] comp = t.getComponentMapping();
            CondensationGraph cg = new CondensationGraph(g, comp);
            Graph dag = cg.build();

            // ===== Topo Sort =====
            Metrics mTopo = new Metrics();
            long t1 = System.nanoTime();
            TopologicalSort topo = new TopologicalSort(dag, mTopo);
            List<Integer> order = topo.kahn();
            long topoTime = (System.nanoTime() - t1) / 1_000_000;

            // ===== Shortest Path =====
            Metrics mSP = new Metrics();
            DAGShortestPath dsp = new DAGShortestPath(dag, mSP);
            int src = (g.getSource() >= 0) ? comp[g.getSource()] : 0;

            long t2 = System.nanoTime();
            var sp = dsp.run(src);
            long spTime = (System.nanoTime() - t2) / 1_000_000;

            long INF = Long.MAX_VALUE / 4;
            int reach = 0;
            double sum = 0;
            long best = Long.MIN_VALUE;
            int bestIdx = -1;

            for (int i = 0; i < sp.dist.length; i++) {
                long d = sp.dist[i];
                if (d != INF) {
                    reach++; sum += d;
                    if (d > best) { best = d; bestIdx = i; }
                }
            }

            long bestDist = 0;
            List<Integer> bestPath = List.of();
            if (bestIdx >= 0) {
                var op = sp.reconstructPath(src, bestIdx);
                if (op.isPresent()) {
                    bestDist = op.get().distance();
                    bestPath = op.get().nodes(); // ✅ fixed
                }
            }

            // ===== Longest Path =====
            Metrics mLP = new Metrics();
            DAGLongestPath lp = new DAGLongestPath(dag, mLP);
            long t3 = System.nanoTime();
            var lpRes = lp.runOptionalSources(Set.of(src));
            long lpTime = (System.nanoTime() - t3) / 1_000_000;

            long crit = 0;
            List<Integer> critPath = List.of();
            var opt = lpRes.reconstructAnyLongest();
            if (opt.isPresent()) {
                crit = opt.get().distance();
                critPath = opt.get().nodes(); // ✅ fixed
            }

            Map<String, Object> row = Map.of(
                    "graph", fname,
                    "n", n,
                    "m", m,
                    "density", density,
                    "scc", Map.of(
                            "time_ms", sccTime,
                            "dfs_visits", mSCC.get("dfsVisits"),
                            "dfs_edges", mSCC.get("dfsEdges"),
                            "count", sccs.size(),
                            "avg_size", sccs.stream().mapToInt(List::size).average().orElse(0)
                    ),
                    "topo", Map.of(
                            "time_ms", topoTime,
                            "pushes", mTopo.get("kahnPushes"),
                            "pops", mTopo.get("kahnPops"),
                            "valid", order.size() == dag.n()
                    ),
                    "shortest_path", Map.of(
                            "time_ms", spTime,
                            "relaxations", mSP.get("relaxations"),
                            "reachable", reach,
                            "avg_distance", reach == 0 ? 0 : sum / reach,
                            "example", Map.of(
                                    "distance", bestDist,
                                    "path", bestPath
                            )
                    ),
                    "longest_path", Map.of(
                            "time_ms", lpTime,
                            "relaxations", mLP.get("long_relaxations"),
                            "critical_length", crit,
                            "critical_path", critPath
                    )
            );

            /** group results */
            String name = fname.toLowerCase();
            if (name.contains("small")) smallResults.add(row);
            else if (name.contains("medium")) mediumResults.add(row);
            else if (name.contains("large")) largeResults.add(row);
            else {
                System.out.println("⚠ Unknown graph type -> default: large");
                largeResults.add(row);
            }
        }

        // ===== Write JSON files =====
        ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();

        writer.writeValue(new File(OUTPUT_DIR + "/results_small.json"), smallResults);
        writer.writeValue(new File(OUTPUT_DIR + "/results_medium.json"), mediumResults);
        writer.writeValue(new File(OUTPUT_DIR + "/results_large.json"), largeResults);

        System.out.println("\n✅ Results saved:");
        System.out.println(" - output/results_small.json");
        System.out.println(" - output/results_medium.json");
        System.out.println(" - output/results_large.json");
    }

    private static int countEdges(Graph g) {
        int c = 0;
        for (int i = 0; i < g.n(); i++) c += g.neighbors(i).size();
        return c;
    }
}
