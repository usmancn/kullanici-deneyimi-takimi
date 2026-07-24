package deneme.Controller;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import deneme.App.GraphBundle;
import deneme.App.RadarFrame;
import deneme.Detection.ObjectDetector;
import deneme.Interfaces.RadarDataSource;
import deneme.Simulation.Simulation;

public class ApplicationController {

    private final Simulation simulation;
    private final RadarDataSource dataSource;
    private final ObjectDetector detector;
    private final GraphBundle graphs;
    private final RadarFrame frame;

    public ApplicationController(
            RadarDataSource dataSource,
            GraphBundle graphs,
            RadarFrame frame
    ) {
        this.simulation = null;
        this.dataSource = dataSource;
        this.detector = null;
        this.graphs = graphs;
        this.frame = frame;
    }

    public ApplicationController(
            Simulation simulation,
            ObjectDetector detector,
            GraphBundle graphs,
            RadarFrame frame
    ) {
        this.simulation = simulation;
        this.dataSource = simulation;
        this.detector = detector;
        this.graphs = graphs;
        this.frame = frame;
    }

    public void install() {
        if (simulation != null && graphs.square != null) {
            graphs.square.installTargetMarkController(simulation);
        }
        if (simulation != null && graphs.circular != null) {
            graphs.circular.installTargetMarkController(simulation);
        }

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stop();
            }
        });
    }

    public void start() {
        frame.setVisible(true);
        if (graphs.firstCanvas() != null) {
            graphs.firstCanvas().requestFocusInWindow();
        }

        graphs.startAnimators();
        graphs.startGraphs();

        if (detector != null) {
            detector.start();
        }
        dataSource.start();
    }

    public void stop() {
        new Thread(() -> {
            dataSource.stop();
            graphs.stopGraphs();
            if (detector != null) {
                detector.stop();
            }
            graphs.stopAnimators();
        }).start();
    }
}
