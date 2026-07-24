package deneme.Controller;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import deneme.App.RadarApp;
import deneme.Detection.ObjectDetector;
import deneme.Simulation.Simulation;

/**
 * Uygulamanin yasam dongusu controller'i: pencere kapanisini yakalar,
 * animator / consumer / dedektor / simulasyonu dogru sirayla acar-kapatir.
 * RadarApp sadece gorunumu (pencere, kartlar, menu) tutar; baslat/durdur
 * mantigi buradadir.
 */
public class ApplicationController {

    private final Simulation simulation;
    private final ObjectDetector detector;   // null: detection kapali
    private final RadarApp app;

    public ApplicationController(Simulation simulation, ObjectDetector detector, RadarApp app) {
        this.simulation = simulation;
        this.detector = detector;
        this.app = app;
    }

    /** Pencere kapanirken her seyin duzgun durmasini baglar. */
    public void install() {
        app.getFrame().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stop();
            }
        });
    }

    /** Pencereyi acar; animator/consumer/dedektor/simulasyonu calistirir. */
    public void start() {
        app.getFrame().setVisible(true);
        app.showFirstCard();

        app.startAnimators();
        app.startConsuming();

        if (detector != null) {
            detector.start();
        }
        simulation.start();
    }

    /** Her seyi durdurur (EDT'yi kilitlememek icin ayri thread'de). */
    public void stop() {
        new Thread(() -> {
            simulation.stop();
            app.stopConsuming();
            if (detector != null) {
                detector.stop();
            }
            app.stopAnimators();
        }).start();
    }
}
