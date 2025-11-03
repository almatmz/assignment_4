import graph.Graph;
import graph.JsonGraphLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JSON loader test aligned with this repo:
 * - Validates via Graph.n() and Graph.neighbors(u)
 * - Expects loader to expose a static load(String) -> Graph
 */
public class JsonLoaderTest {

    @Test
    void loadsGraphFromJsonAndMatchesCounts() throws IOException {
        String json = """
        {
          "n": 5,
          "edges": [
            {"u":0,"v":1,"w":2},
            {"u":1,"v":2,"w":3},
            {"u":2,"v":3,"w":1},
            {"u":3,"v":4,"w":4}
          ]
        }
        """;

        Path temp = Files.createTempFile("graph-", ".json");
        Files.writeString(temp, json);

        try {
            Graph g = JsonGraphLoader.load(temp.toString());
            assertEquals(5, g.n(), "Node count mismatch");
            assertEquals(4, countEdges(g), "Edge count mismatch");
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static int countEdges(Graph g) {
        int m = 0;
        for (int u = 0; u < g.n(); u++) m += g.neighbors(u).size();
        return m;
    }
}