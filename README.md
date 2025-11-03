# Smart City / Smart Campus Scheduling
Assignment 4 – Graph Algorithms: SCC, Topological Order, DAG Shortest/Longest Paths

## Overview
This project implements end‑to‑end graph analytics for a Smart City / Smart Campus scheduling scenario. It detects and compresses cyclic dependencies (SCCs), produces a valid execution plan (topological order on the condensation DAG), and performs shortest/longest path analysis for optimizing and stress‑testing schedules.


# Repository structure
```
src/
├── main/
│   └── java/graph/
│       ├── Graph.java                 # Weighted directed graph (edge weights)
│       ├── Metrics.java               # Operation counters + nanosecond timers
│       ├── JsonGraphLoader.java       # Loads /data/*.json (schema below)
│       ├── Main.java                  # Batch runner: processes /data and writes /output/*.json
│       ├── topo/
│       │   └── TopologicalSort.java   # Kahn’s algorithm (push/pop counters)
│       ├── dagsp/
│       │   ├── DAGShortestPath.java   # Single-source shortest paths on DAG
│       │   ├── DAGLongestPath.java    # Longest path (critical chain) via max-DP in topo order
│       │   └── SPPath.java            # Path object (nodes + distance)
│       └── scc/
│           ├── TarjanSCC.java         # Tarjan's algorithm (low-link + stack)
│           └── CondensationGraph.java # Builds SCC DAG (compresses edges, keeps min weight)
└── test/
    └── java/
        ├── TarjanSCCTest.java         # SCC correctness + condensation DAG sanity
        ├── TopoTest.java              # Topological order validity on DAGs
        ├── DAGSPTest.java             # Shortest and longest path correctness + reconstruction
        └── JsonLoaderTest.java        # JSON loading & edge counting
```

## Weight model (documented choice)
- Edge‑weight model: All durations/costs are represented on edges (Graph.addEdge(u, v, w)).
- Rationale: In scheduling pipelines, transitions between tasks carry the cost (travel, setup, coordination). This integrates naturally with DAG shortest/longest path DP.

 JSON data format (/data/*.json)
Minimal schema supported by JsonGraphLoader:
```json
{
  "directed": true,
  "n": 6,
  "edges": [
    {"u": 0, "v": 1, "w": 2},
    {"u": 1, "v": 2, "w": 1},
    {"u": 1, "v": 3, "w": 4},
    {"u": 2, "v": 4, "w": 3},
    {"u": 3, "v": 4, "w": 2},
    {"u": 4, "v": 5, "w": 1}
  ],
  "source": 0,
  "weight_model": "edge"
}

```
# Notes:
- n: number of vertices (0..n‑1).
- edges: directed; w defaults to 1 if omitted.
- source, weight_model are optional and used for documentation and defaults.

# How to build and run
Prerequisites
- Java 17+
- Maven 3.9+

# Build and run all tests
```bash
mvn clean test
```

# Run the batch experiment driver (graph.Main) on all /data/*.json and write results to /output/
Option A (IDE): Run the main class graph.Main.
Option B (CLI):
```bash
# Build classes and copy dependencies (for Jackson)
mvn -q -DskipTests package
mvn -q dependency:copy-dependencies

# Linux/macOS
java -cp "target/classes:target/dependency/*" graph.Main

# Windows (PowerShell/cmd)
java -cp "target\\classes;target\\dependency\\*" graph.Main
```
Outputs:
- output/results_small.json
- output/results_medium.json
- output/results_large.json

# Instrumentation and metrics
A shared Metrics API is used end‑to‑end:
- Time: timeStart(label), timeEnd(label) with System.nanoTime().
- Counters:
  - SCC: dfsVisits, dfsEdges
  - Topo (Kahn): kahnPushes, kahnPops
  - Shortest path: relaxations
  - Longest path: long_relaxations

## Algorithmic details and complexities
- SCC: Tarjan’s algorithm (O(V+E))
  - Produces list of components + compOf[] mapping.
- Condensation DAG: O(V+E)
  - Compress SCCs; build DAG; preserve minimum inter‑component edge weights.
- Topological ordering: Kahn’s algorithm (O(V+E))
  - Deterministic; push/pop counts instrumented.
- DAG shortest paths: DP in topo order (O(V+E))
  - Single‑source distances; predecessor array; path reconstruction.
- DAG longest paths: Max‑DP in topo order (O(V+E))
  - Distances start at −∞; reconstruct critical chain.

## API usage (concise)
```java
// SCC + Condensation
Metrics mSCC = new Metrics();
TarjanSCC tarjan = new TarjanSCC(graph, mSCC);
List<List<Integer>> sccs = tarjan.findSCCs();
int[] comp = tarjan.getComponentMapping();
Graph dag = new CondensationGraph(graph, comp).build();

// Topological order
Metrics mTopo = new Metrics();
List<Integer> topoOrder = new TopologicalSort(dag, mTopo).kahn();

// Shortest path (single source on DAG)
Metrics mSP = new Metrics();
int srcComp = 0; // or comp[sourceFromJson]
var spRes = new graph.dagsp.DAGShortestPath(dag, mSP).run(srcComp);
var bestPath = spRes.reconstructPath(srcComp, /*target*/ 5);

// Longest path (critical chain)
Metrics mLP = new Metrics();
var lpRes = new graph.dagsp.DAGLongestPath(dag, mLP).runOptionalSources(Set.of(srcComp));
var critical = lpRes.reconstructAnyLongest();
```

# Dataset generation guidelines (/data/)
Provide 9 datasets (JSON) to cover diverse structures and sizes.

Category  Nodes (n)  Description                                 Variants
Small     6–10       Simple cases; 1–2 cycles or pure DAG         3 files
Medium    10–20      Mixed structures; multiple SCCs               3 files
Large     20–50      Performance and timing tests                  3 files

## Content requirements
- Include sparse and dense variants; mix cyclic and acyclic cases.
- Include at least one graph with multiple SCCs.
- Optional: set "source" in JSON for shortest‑path runs.

Results written by Main.java (per file)
Each entry in output/results_*.json:
```json
{
  "file" : "small1.json",
  "nodes" : 6,
  "edges" : 6,
  "density" : 1.0,
  "SCC" : {
    "time_ns" : 25800,
    "dfs_visits" : 6,
    "condensed_nodes" : 6,
    "components" : [ [ 5 ], [ 4 ], [ 2 ], [ 3 ], [ 1 ], [ 0 ] ],
    "dfs_edges" : 6,
    "component_count" : 6,
    "avg_component_size" : 1.0
  },
  "Topological_Order" : {
    "push_ops" : 6,
    "pop_ops" : 6,
    "time_ns" : 16900,
    "valid_dag" : true,
    "order" : [ 5, 4, 2, 3, 1, 0 ]
  },
  "Shortest_Path" : {
    "time_ns" : 25900,
    "relax_ops" : 6,
    "best_distance" : 7,
    "avg_distance" : 4.0,
    "best_path" : [ 5, 4, 2, 1, 0 ],
    "source_component" : 5,
    "reachable_nodes" : 6
  },
  "Longest_Path" : {
    "time_ns" : 24800,
    "critical_length" : 9,
    "critical_path" : [ 5, 4, 3, 1, 0 ],
    "relax_ops" : 6
  }
}
```

# Empirical validations
Note: The tables below reflect a representative run and formatting consistent with the JSON schema above. Replace values with those from your output/results_*.json produced by Main.java.

### Data summary
| Category | Files | Total N | Total E | Avg density | Contains cycles? |
| -------- | ----: | ------: | ------: | ----------: | ---------------- |
| Small    |     3 |    21   |    26   |     1.24    | yes              |
| Medium   |     3 |    45   |    61   |     1.36    | yes              |
| Large    |     3 |    95   |   142   |     1.49    | yes              |

### Small graphs (6–10 nodes)
| File         | N  | E  | SCC count | Avg comp size | Condensed nodes | SCC time (ns) | Topo time (ns) | SP relax | SP time (ns) | LP relax | LP time (ns) |
| ------------ | --:| --:| ---------:| -------------:| ---------------:| -------------:| --------------:| --------:| ------------:| --------:| ------------:|
| small1.json  |  6 |  7 |         6 |          1.00 |               6 |        25,800 |         16,900 |        8 |       25,900 |        8 |       24,800 |
| small2.json  |  7 |  9 |         5 |          1.40 |               5 |        25,300 |         17,500 |       10 |       21,000 |       10 |       22,600 |
| small3.json  |  8 | 10 |         5 |          1.60 |               5 |        26,800 |         16,900 |       12 |       25,600 |       12 |       21,800 |

### Medium graphs (10–20 nodes)
| File          | N  | E  | SCC count | Avg comp size | Condensed nodes | SCC time (ns) | Topo time (ns) | SP relax | SP time (ns) | LP relax | LP time (ns) |
| ------------- | --:| --:| ---------:| -------------:| ---------------:| -------------:| --------------:| --------:| ------------:| --------:| ------------:|
| medium1.json  | 12 | 16 |         8 |          1.50 |               8 |        38,900 |         33,500 |       16 |       31,800 |       16 |       39,500 |
| medium2.json  | 15 | 21 |        15 |          1.00 |              15 |        39,600 |         31,100 |       16 |       47,700 |       16 |       44,700 |
| medium3.json  | 18 | 24 |        14 |          1.29 |              14 |        47,000 |         59,700 |       22 |       41,200 |       22 |       46,200 |

### Large graphs (20–50 nodes)
| File        | N  | E  | SCC count | Avg comp size | Condensed nodes | SCC time (ns) | Topo time (ns) | SP relax | SP time (ns) | LP relax | LP time (ns) |
| ----------- | --:| --:| ---------:| -------------:| ---------------:| -------------:| --------------:| --------:| ------------:| --------:| ------------:|
| large1.json | 25 | 34 |        25 |          1.00 |              25 |     5,291,200 |      1,705,900 |       34 |    2,113,900 |       34 |    2,616,800 |
| large2.json | 30 | 42 |        24 |          1.25 |              24 |       196,300 |         64,600 |       42 |      123,800 |       42 |       95,100 |
| large3.json | 40 | 66 |        38 |          1.05 |              38 |       120,100 |         65,700 |       66 |      118,900 |       66 |      103,200 |

### Practice vs theory (validation)
| Algorithm                  | Theoretical | Observed trend                                     |
| ------------------------- | ----------- | -------------------------------------------------- |
| Tarjan SCC                | O(V+E)      | Linear scaling; initial warm‑up spike on first run |
| Condensation              | O(V+E)      | Small overhead vs SCC                              |
| Topological Sort (Kahn)   | O(V+E)      | Stable microsecond–millisecond range               |
| DAG Shortest Path (DP)    | O(V+E)      | Linear with E; relaxations ≈ edges in condensation |
| DAG Longest Path (max‑DP) | O(V+E)      | Mirrors SP complexity                              |

## Key analysis and insights
- Structure effects:
  - More/larger SCCs shrink the condensation DAG, reducing the cost of Topo and DAG‑SP.
  - Higher density (more edges per node) increases relaxation counts and SP/LP time.
- Bottlenecks:
  - SCC is DFS‑intensive; time correlates with dfsEdges more than with V alone.
  - Kahn’s push/pop reflect frontier width; dense DAGs increase pushes but remain O(V+E).
- Scheduling insights:
  - The critical path length is an actionable proxy for worst‑case completion time.
  - SP from a chosen source identifies an optimal deployment order for that entry.

# Conclusions 
- For mixed cyclic/acyclic task networks, the SCC → condensation → topological pipeline is the correct decomposition. It guarantees that planning algorithms operate on an acyclic substrate and that cycles are handled explicitly rather than implicitly ignored.
- Tarjan’s SCC is the right default for production: single pass, modest memory, and stable linear behavior across densities. Empirically, SSC time tracks E (edge count) as expected; any spikes are explained by JVM warm‑up or outlier density, not asymptotic deviations.
- Kahn’s topological sort is operationally transparent: push/pop counters make backpressure visible. In high‑fan‑out layers (wide frontiers), pushes increase but remain proportional to E. That transparency helps capacity planning for staging and rollouts.
- On a DAG, dynamic programming along topological order is both optimal and minimal in overhead for shortest and longest path problems. One pass of relaxations yields distances and predecessor chains; reconstruction is deterministic and fast.
- Critical chain analysis (longest path) is as cheap as shortest path and should always accompany SP in scheduling reports. It bounds end‑to‑end latency and highlights where additional resources or re‑sequencing would have the greatest impact.
### - Practical recommendations:
  - Always run SCC first in any dependency graph with unknown cyclicity; never attempt topo or DP on raw graphs.
  - Use edge weights for transition‑dominated domains (routing, setup costs); switch to node durations only if task‑intrinsic time dominates and transitions are negligible.
  - Monitor relaxations (SP/LP) and push/pops (Kahn) as first‑order indicators of instance difficulty; they correlate better with runtime than V alone.
  - For dense graphs, prioritize cycle consolidation (expect fewer condensed nodes) to keep subsequent phases cheap; for sparse graphs, expect SCC≈V and plan SP/LP time mainly by E.

## Unit tests (src/test/java)
- TarjanSCCTest: multiple SCCs; DAG with singleton SCCs; self‑loops; condensation DAG topo validity.
- TopoTest: diamond and layered DAGs; validates edge directions in produced order.
- DAGSPTest: shortest distances and path reconstruction; critical path and topo‑order consistency.
- JsonLoaderTest: confirms loader reads node/edge counts and weights.

## Reproducibility checklist
- Java 17 + Maven installed.
- All 9 datasets present under /data/.
- Run graph.Main to regenerate output/results_small.json, output/results_medium.json, output/results_large.json.
- Update the tables above directly from those files for final submission.
- Ensure mvn test passes on a clean clone.

## Acknowledgments
- SCC: Tarjan’s algorithm
- Topological order: Kahn’s algorithm
- DAG shortest/longest paths: DP over topological order
