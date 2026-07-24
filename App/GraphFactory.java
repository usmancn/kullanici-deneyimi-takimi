package deneme.App;

import java.util.List;

import com.jogamp.opengl.GLCapabilities;

import deneme.Simulation.GainFilterModel;

public final class GraphFactory {

    private GraphFactory() {
    }

    public static GraphBundle createDefault(
            GLCapabilities caps,
            RadarQueues queues,
            int fps,
            GainFilterModel gainFilter
    ) {
        return new GraphBundle(caps, queues, fps, gainFilter);
    }

    public static GraphBundle createCustom(List<GraphDefinition> definitions, int fps) {
        return new GraphBundle(definitions, fps);
    }
}
