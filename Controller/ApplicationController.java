package deneme.Controller;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import deneme.App.GraphBundle;
import deneme.App.RadarFrame;
import deneme.Detection.ObjectDetector;
import deneme.Simulation.Simulation;

public class ApplicationController {

    private final Simulation simulation;
    private final ObjectDetector detector;
    private final GraphBundle graphs;
    private final RadarFrame frame;

    public ApplicationController(
            Simulation simulation,
            ObjectDetector detector,
            GraphBundle graphs,
            RadarFrame frame
    ) {
        this.simulation = simulation;
        this.detector = detector;
        this.graphs = graphs;
        this.frame = frame;
    }

    public void install() {
        graphs.square.installTargetMarkController(simulation);
        graphs.circular.installTargetMarkController(simulation);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stop();
            }
        });
    }

    public void start() {
        frame.setVisible(true);
        graphs.square.requestFocusInWindow();

        graphs.startAnimators();
        graphs.startGraphs();

        detector.start();
        simulation.start();
    }

    public void stop() {
        new Thread(() -> {
            simulation.stop();
            graphs.stopGraphs();
            detector.stop();
            graphs.stopAnimators();
        }).start();
    }
}