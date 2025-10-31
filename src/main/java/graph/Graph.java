package graph;

import java.util.*;

public class Graph {
    public static class Edge {
        public final int to;
        public final long weight;
        public Edge(int to, long weight) { this.to = to; this.weight = weight; }
    }

    private final int n;
    private final List<List<Edge>> adj;
    private int source = -1;
    private String weightModel = "edge"; // "edge" or "node"

    public Graph(int n) {
        this.n = n;
        this.adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    }

    public int n() { return n; }

    public void addEdge(int u, int v, long w) {
        adj.get(u).add(new Edge(v, w));
    }

    public List<Edge> neighbors(int u) { return Collections.unmodifiableList(adj.get(u)); }

    public void setSource(int s) { this.source = s; }
    public int getSource() { return source; }

    public void setWeightModel(String model) { this.weightModel = model; }
    public String getWeightModel() { return weightModel; }
}
