package deneme.App;

import java.io.File;
import java.io.IOException;

import javax.swing.SwingUtilities;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

import deneme.Detection.ObjectDetector;
import deneme.MessageProcess.MessagePublisher;
import deneme.Simulation.Simulation;
import deneme.Controller.ApplicationController;
import deneme.Simulation.GainFilterModel;
import deneme.Simulation.ImageDataSource;

public class Main{

    private static final int FPS = 40;
    private static final int DEFAULT_TARGET_COUNT = 15;
    private static final File DEMO_IMAGE_FILE = new File("assets/shhh-dog-shhh.png");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        StartupDialog.DataMode dataMode = StartupDialog.askDataMode();
        if (dataMode == StartupDialog.DataMode.CANCEL) {
            return;
        }

        // ---- 2) mesaj hatti: veri kaynagi -> graph queue'lari ----
        RadarQueues queues = new RadarQueues();

        MessagePublisher publisher = new MessagePublisher();

        // heavyweight popup: GLCanvas uzerinde hafif popup'lar gorunmez
        javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        GainFilterModel gainFilter = new GainFilterModel();

        // ---- 3) OpenGL graph'larini kur ----
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities caps = new GLCapabilities(profile);
        
        GraphBundle graphs = GraphFactory.createDefault(caps, queues, FPS, gainFilter);
        
        // ---- 4) pencereyi kur ----	
        GainFilterSlider gainSlider = new GainFilterSlider(gainFilter);
        RadarFrame frame = new RadarFrame(graphs, gainSlider);

        ApplicationController appController = createApplicationController(
                dataMode,
                publisher,
                queues,
                graphs,
                frame
        );
        if (appController == null) {
            return;
        }

        appController.install();
        appController.start(); 
    }

    private static ApplicationController createApplicationController(
            StartupDialog.DataMode dataMode,
            MessagePublisher publisher,
            RadarQueues queues,
            GraphBundle graphs,
            RadarFrame frame
    ) {
        if (dataMode == StartupDialog.DataMode.IMAGE_DEMO) {
            queues.subscribeGraphs(publisher);
            try {
                ImageDataSource imageDataSource = new ImageDataSource(DEMO_IMAGE_FILE, publisher, FPS);
                return new ApplicationController(imageDataSource, graphs, frame);
            } catch (IOException ex) {
                StartupDialog.showError("Fotograf okunamadi: " + DEMO_IMAGE_FILE.getPath());
                return null;
            }
        }

        int targetCount = StartupDialog.askTargetCount(DEFAULT_TARGET_COUNT);
        if (targetCount < 0) {
            return null;
        }

        queues.subscribeAll(publisher);

        Simulation simulation = new Simulation(targetCount, publisher);

        // scanline tabanli obje dedektoru: obje bulunca Simulation'a ID sorar
        ObjectDetector detector = new ObjectDetector(queues.detection, simulation);

        return new ApplicationController(simulation, detector, graphs, frame);
    }

}
