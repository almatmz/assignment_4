package graph.dagsp;

import java.util.*;

public class Path {
    private final List<Integer> nodes;
    private final long dist;

    public Path(List<Integer> nodes, long dist) {
        this.nodes = nodes;
        this.dist = dist;
    }

    public List<Integer> nodes() { return nodes; }
    public long distance() { return dist; }

    @Override
    public String toString() {
        return "Path{dist=" + dist + ", nodes=" + nodes + '}';
    }
}
