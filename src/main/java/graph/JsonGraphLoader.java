package graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class JsonGraphLoader {
    private static final ObjectMapper M = new ObjectMapper();

    public static Graph load(String path) throws IOException {
        JsonNode root = M.readTree(new File(path));
        int n = root.get("n").asInt();
        Graph g = new Graph(n);

        JsonNode edges = root.withArray("edges");
        for (JsonNode e : edges) {
            int u = e.get("u").asInt();
            int v = e.get("v").asInt();
            long w = e.has("w") ? e.get("w").asLong() : 1L;
            g.addEdge(u, v, w);
        }

        if (root.has("source")) g.setSource(root.get("source").asInt());
        if (root.has("weight_model")) g.setWeightModel(root.get("weight_model").asText());
        return g;
    }
}
