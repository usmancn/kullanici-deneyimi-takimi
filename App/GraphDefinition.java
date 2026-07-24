package deneme.App;

import deneme.Interfaces.RadarGraph;

public final class GraphDefinition {

    private final String id;
    private final String title;
    private final RadarGraph graph;

    public GraphDefinition(String id, String title, RadarGraph graph) {
        this.id = id;
        this.title = title;
        this.graph = graph;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public RadarGraph graph() {
        return graph;
    }
}
