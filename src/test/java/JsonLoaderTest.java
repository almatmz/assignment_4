import graph.Graph;
import graph.JsonGraphLoader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;

public class JsonLoaderTest {

    @Test
    void testLoadSimpleJson() throws IOException {
        String path = "data/test_small.json";
        Graph g = JsonGraphLoader.load(path);
        assertNotNull(g);
        assertTrue(g.n() > 0);
    }
}
