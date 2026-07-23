package deneme.App;

import javax.swing.SwingUtilities;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

import deneme.Detection.ObjectDetector;
import deneme.MessageProcess.MessagePublisher;
import deneme.Simulation.Simulation;
import deneme.Controller.ApplicationController;

public class Main{

    private static final int FPS = 40;
    private static final int DEFAULT_TARGET_COUNT = 15;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
    	// ---- 1) baslangic ekrani: hedef sayisi ----
    	int targetCount = StartupDialog.askTargetCount(DEFAULT_TARGET_COUNT);
    	if (targetCount < 0) {
            return; // kullanici iptal etti
        }

        // ---- 2) mesaj hatti: Simulation -> (square queue, waterfall queue) ----
        RadarQueues queues = new RadarQueues();

        MessagePublisher publisher = new MessagePublisher();
        queues.subscribeAll(publisher);

        Simulation simulation = new Simulation(targetCount, publisher);

        // scanline tabanli obje dedektoru: obje bulunca Simulation'a ID sorar
        ObjectDetector detector = new ObjectDetector(queues.detection, simulation);
        
        // heavyweight popup: GLCanvas uzerinde hafif popup'lar gorunmez
        javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        // ---- 3) OpenGL graph'larini kur ----
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities caps = new GLCapabilities(profile);
        
        GraphBundle graphs = new GraphBundle(caps, queues, FPS);

        // ---- 4) pencereyi kur ----	
        GainFilterSlider gainSlider = new GainFilterSlider();
        RadarFrame frame = new RadarFrame(graphs, gainSlider);
        
        ApplicationController appController = new ApplicationController(simulation, detector, graphs, frame);
        appController.install();
        appController.start(); 
    }

}