package deneme.App;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import deneme.Graph.Circular.CircularCanvas;
import deneme.Graph.Line.LineCanvas;
import deneme.Graph.Square.SquareCanvas;
import deneme.Graph.Waterfall.WaterfallCanvas;
import deneme.Interfaces.RadarGraph;
import deneme.Simulation.GainFilterModel;

public class GraphBundle {

    public final SquareCanvas square;
    public final WaterfallCanvas waterfall;
    public final LineCanvas line;
    public final CircularCanvas circular;

    public final List<GraphDefinition> definitions;
    public final List<RadarGraph> graphs;
    public final List<FPSAnimator> animators;

    public GraphBundle(GLCapabilities caps, RadarQueues queues, int fps, GainFilterModel gainFilter) {
    	square = new SquareCanvas(caps, queues.square, gainFilter);
    	waterfall = new WaterfallCanvas(caps, queues.waterfall);
    	line = new LineCanvas(caps, queues.line, gainFilter);
    	circular = new CircularCanvas(caps, queues.circular, gainFilter);

        definitions = List.of(
            new GraphDefinition("square", "Square", square),
            new GraphDefinition("line-waterfall", "Line + Waterfall", line),
            new GraphDefinition("circular", "Circular", circular)
        );

        graphs = List.of(
            square,
            waterfall,
            line,
            circular
        );

        animators = List.of(
            new FPSAnimator(square, fps, true),
            new FPSAnimator(waterfall, fps, true),
            new FPSAnimator(line, fps, true),
            new FPSAnimator(circular, fps, true)
        );
    }

    public GraphBundle(List<GraphDefinition> definitions, int fps) {
        this.square = null;
        this.waterfall = null;
        this.line = null;
        this.circular = null;
        this.definitions = List.copyOf(definitions);

        List<RadarGraph> graphList = new ArrayList<>();
        List<FPSAnimator> animatorList = new ArrayList<>();
        for (GraphDefinition definition : definitions) {
            RadarGraph graph = definition.graph();
            graphList.add(graph);
            animatorList.add(new FPSAnimator(graph.canvas(), fps, true));
        }

        this.graphs = List.copyOf(graphList);
        this.animators = List.copyOf(animatorList);
    }

    public GLCanvas firstCanvas() {
        if (graphs.isEmpty()) {
            return null;
        }
        return graphs.get(0).canvas();
    }

    public void startGraphs() {
        for (RadarGraph graph : graphs) {
            graph.startConsuming();
        }
    }

    public void stopGraphs() {
        for (RadarGraph graph : graphs) {
            graph.stopConsuming();
        }
    }

    public void startAnimators() {
        for (FPSAnimator animator : animators) {
            animator.start();
        }
    }

    public void stopAnimators() {
        for (FPSAnimator animator : animators) {
            animator.stop();
        }
    }
}
