package deneme.App;

import java.util.List;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.util.FPSAnimator;

import deneme.Graph.Circular.CircularCanvas;
import deneme.Graph.Line.LineCanvas;
import deneme.Graph.Square.SquareCanvas;
import deneme.Graph.Waterfall.WaterfallCanvas;
import deneme.Interfaces.GraphLifecycle;

public class GraphBundle {

    public final SquareCanvas square;
    public final WaterfallCanvas waterfall;
    public final LineCanvas line;
    public final CircularCanvas circular;

    public final List<GraphLifecycle> graphs;
    public final List<FPSAnimator> animators;

    public GraphBundle(GLCapabilities caps, RadarQueues queues, int fps) {
        square = new SquareCanvas(caps, queues.square);
        waterfall = new WaterfallCanvas(caps, queues.waterfall);
        line = new LineCanvas(caps, queues.line);
        circular = new CircularCanvas(caps, queues.circular);

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

    public void startGraphs() {
        for (GraphLifecycle graph : graphs) {
            graph.startConsuming();
        }
    }

    public void stopGraphs() {
        for (GraphLifecycle graph : graphs) {
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