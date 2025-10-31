
#  Smart City / Smart Campus Scheduling

### Assignment 4 – Graph Algorithms: SCC, Topological Order & DAG Shortest/Longest Paths

---

##  Overview

This project implements **graph analytics** for Smart City / Smart Campus scheduling systems.
It models dependencies between tasks (street cleaning, camera maintenance, repair scheduling, etc.) and provides tools for:

1. Detecting **Strongly Connected Components (SCCs)** to identify cyclic dependencies.
2. Building a **Condensation DAG** and computing its **Topological Order**.
3. Computing **Shortest and Longest Paths** in the resulting DAG for optimal or critical scheduling.

The system is designed with modular packages, efficient metrics instrumentation, and clear separation of algorithmic stages.

---

##  Packages and Structure

```
graph/
├── Graph.java                 # Core weighted directed graph
├── Metrics.java               # Operation counters + timers
├── TopologicalSort.java       # Kahn/DFS topological ordering
├── dagsp/
│   ├── DAGShortestPath.java   # Single-source shortest path in DAG
│   ├── DAGLongestPath.java    # Longest/critical path via DP
│   └── SPPath.java            # Path reconstruction utility
└── scc/
    ├── TarjanSCC.java         # Strongly Connected Components (Tarjan)
    └── CondensationGraph.java # Builds DAG of SCCs
```

All datasets are under `/data/`, grouped into:

* `smallX.json` (6–10 nodes)
* `mediumX.json` (10–20 nodes)
* `largeX.json` (20–50 nodes)

---

## ⚙️ Algorithms & Design Choices

### 1. **SCC Detection – Tarjan’s Algorithm**

* **Why Tarjan:**
  It performs a single DFS pass (`O(V + E)`), using low-link values to detect SCC roots efficiently.
  Compared to Kosaraju, it avoids two traversals and is memory-efficient.
* **Output:**

  * List of components (each as a vertex group)
  * Component mapping array
  * Metrics: DFS visits, DFS edges, total time

```java
TarjanSCC tarjan = new TarjanSCC(graph, metrics);
List<List<Integer>> components = tarjan.findSCCs();
```

### 2. **Condensation Graph Construction**

After detecting SCCs, each component becomes a **node** in a new **DAG**.
Edges are added between components if an edge connects nodes from different SCCs.

* Duplicate edges between components are minimized by storing minimal weights.
* Used for all subsequent DAG processing.

```java
CondensationGraph builder = new CondensationGraph(originalGraph, compMap);
Graph dag = builder.build();
```

### 3. **Topological Sorting – Kahn’s Algorithm**

* **Why Kahn’s:**
  It is deterministic, straightforward, and provides natural push/pop metrics.
* **Metrics tracked:**

  * `push_ops`, `pop_ops`
  * Validation of DAG correctness

```java
TopologicalSort topo = new TopologicalSort(dag, metrics);
List<Integer> order = topo.kahn();
```

### 4. **Shortest Path in DAG – Dynamic Programming**

* **Approach:**
  Process vertices in topological order, relaxing outgoing edges once per node.
* **Complexity:** `O(V + E)`
* **Weights used:** edge weights (documented choice).
* **Output:** distance array, predecessor array, and one optimal path.

```java
DAGShortestPath sp = new DAGShortestPath(dag, metrics);
Result result = sp.run(source);
Optional<SPPath> path = result.reconstructPath(src, tgt);
```

### 5. **Longest Path (Critical Path)**

* **Approach:**
  Similar to shortest path, but with reversed comparison (`>` instead of `<`).
  Distances initialized to `-∞` and updated over the same topological order.
* **Use case:**
  Identify **critical chain** of dependent tasks that define total schedule length.

```java
DAGLongestPath lp = new DAGLongestPath(dag, metrics);
Optional<SPPath> critical = lp.runOptionalSources(null)
                              .reconstructAnyLongest();
```

---

## 📊 Dataset Summary

| File         | Nodes | Edges | Density | #Components | Avg Comp. Size | DAG Valid | Best Shortest Distance | Critical Path Length |
| ------------ | ----- | ----- | ------- | ----------- | -------------- | --------- | ---------------------- | -------------------- |
| small1.json  | 6     | 6     | 1.0     | 6           | 1.0            | ✅         | 7                      | 9                    |
| small2.json  | 7     | 7     | 1.0     | 5           | 1.4            | ✅         | 6                      | 6                    |
| small3.json  | 8     | 8     | 1.0     | 5           | 1.6            | ✅         | 4                      | 4                    |
| medium1.json | 12    | 12    | 1.0     | 8           | 1.5            | ✅         | 5                      | 5                    |
| medium2.json | 15    | 16    | 1.07    | 15          | 1.0            | ✅         | 11                     | 11                   |
| medium3.json | 18    | 18    | 1.0     | 14          | 1.29           | ✅         | 13                     | 13                   |
| large1.json  | 25    | 25    | 1.0     | 25          | 1.0            | ✅         | 44                     | 44                   |
| large2.json  | 30    | 30    | 1.0     | 24          | 1.25           | ✅         | 42                     | 42                   |
| large3.json  | 40    | 40    | 1.0     | 38          | 1.05           | ✅         | 66                     | 66                   |

🧾 **Observations**

* SCC count decreases with denser graphs containing more cycles.
* DAG topological order was valid in all datasets.
* Longest paths scale linearly with node count, indicating balanced graph generation.

---

## ⏱️ Metrics & Instrumentation

Each algorithm class uses a shared `Metrics` object:

* `timeStart(label)` / `timeEnd(label)` — records duration in nanoseconds.
* `inc(counterName)` — increments operation counters:

  * `dfsVisits`, `dfsEdges` for SCC
  * `push_ops`, `pop_ops` for Topo
  * `relax_ops`, `long_relaxations` for DAG-SP

Example metrics from `medium2.json`:

```
SCC: dfs_visits=15, dfs_edges=16
Topo: push_ops=15, pop_ops=15
Shortest Path: relax_ops=16, time=0ms
Longest Path: relax_ops=16, time=0ms
```

---

## 🚀 Running the Project

### 🛠️ Build & Run

```bash
# Clone repository
git clone https://github.com/<your-username>/SmartCityGraphScheduler.git
cd SmartCityGraphScheduler

# Compile
mvn clean install

# Run main tests
java -jar target/SmartCityGraphScheduler.jar
```

### 🧪 JUnit Tests

Located under `src/test/java/`.
They include:

* Deterministic small graphs
* Cyclic vs acyclic cases
* Edge-weighted DAG shortest path verification

Run with:

```bash
mvn test
```

---

## 📈 Analysis & Discussion

| Aspect               | Observation                       | Implication                        |
| -------------------- | --------------------------------- | ---------------------------------- |
| **SCC bottleneck**   | DFS-heavy; scales linearly with E | Efficient for large sparse graphs  |
| **Topo sorting**     | Negligible overhead               | Ideal for scheduling tasks         |
| **Shortest path**    | Linear-time over DAG              | Best for dependency resolution     |
| **Longest path**     | Same complexity                   | Key for identifying critical tasks |
| **Structure effect** | More SCCs → smaller DAG           | Reduces subsequent workload        |

---

## 💡 Conclusions

* **Tarjan’s SCC** is optimal for cyclic dependency detection in service graphs.
* **Condensation DAG + Kahn Sort** provides a clean schedule order free of cycles.
* **Dynamic Programming over DAG** efficiently computes both minimal and critical task chains.
* The unified metrics system enables fine-grained performance analysis.

✅ **When to use which:**

* Use **Tarjan** for cyclic graphs (task dependency detection).
* Use **Kahn Topo + DAG-SP** for acyclic planning (optimal scheduling).
* Use **Longest Path** for critical chain analysis (deadline optimization).

---

