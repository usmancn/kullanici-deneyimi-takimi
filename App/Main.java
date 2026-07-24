package deneme.App;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import deneme.Builder.AppBuilder;
import deneme.Builder.CircularCanvasBuilder;
import deneme.Builder.GainSliderBuilder;
import deneme.Builder.LineCanvasBuilder;
import deneme.Builder.SimulationBuilder;
import deneme.Builder.SquareCanvasBuilder;
import deneme.Builder.WaterfallCanvasBuilder;

public class Main {

    private static final int FPS = 40;
    private static final int DEFAULT_TARGET_COUNT = 15;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        // ---- 1) baslangic ekrani: hedef sayisi ----
        int targetCount = askTargetCount();
        if (targetCount < 0) {
            return; // kullanici iptal etti
        }

        // ---- 2) tum uygulama builder uzerinden kurulur ----
        RadarApp app = new AppBuilder()
                .title("Radar - Square / Waterfall")
                .simulation(new SimulationBuilder()
                        .FPS(FPS)
                        .EnemyCount(targetCount))
                .addSquareCanvas(new SquareCanvasBuilder())
                .addLineCanvas(new LineCanvasBuilder())
                .addWaterfallCanvas(new WaterfallCanvasBuilder())
                .addCircularCanvas(new CircularCanvasBuilder())
                .gainSlider(new GainSliderBuilder())
                .gainSliderPosition(SliderPosition.BOTTOM)
                .animatorFps(FPS)
                .build();

        // ---- 3) pencereyi ac, her sey calissin ----
        app.start();
    }

    // hedef sayisini soran ufak baslangic ekrani
    private static int askTargetCount() {
        String input = JOptionPane.showInputDialog(
                null,
                "Hedef sayisi:",
                "Baslangic",
                JOptionPane.QUESTION_MESSAGE);

        if (input == null) {
            return -1; // iptal / kapatma
        }
        try {
            return Math.max(0, Integer.parseInt(input.trim()));
        } catch (NumberFormatException ex) {
            return DEFAULT_TARGET_COUNT; // gecersiz giris -> varsayilan
        }
    }
}
