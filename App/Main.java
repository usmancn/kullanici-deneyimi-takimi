package deneme.App;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.FPSAnimator;

import deneme.Graph.Line.LineCanvas;
import deneme.Graph.Square.RadarCanvas;
import deneme.Graph.Waterfall.WaterfallCanvas;
import deneme.MessageProcess.MessagePublisher;
import deneme.MessageProcess.QueueMessage;
import deneme.Simulation.Simulation;

public class Main {

    private static final int FPS = 40;
    private static final int DEFAULT_TARGET_COUNT = 15;

    private static final String CARD_SQUARE = "square";
    private static final String CARD_WATERFALL = "waterfall";
    private static final String CARD_LINE = "line";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        // ---- 1) baslangic ekrani: hedef sayisi ----
        int targetCount = askTargetCount();
        if (targetCount < 0) {
            return;   // kullanici iptal etti
        }

        // ---- 2) mesaj hatti: Simulation -> (square queue, waterfall queue) ----
        BlockingQueue<QueueMessage> squareQueue    = new LinkedBlockingQueue<>();
        BlockingQueue<QueueMessage> waterfallQueue = new LinkedBlockingQueue<>();
        BlockingQueue<QueueMessage> lineQueue      = new LinkedBlockingQueue<>();

        MessagePublisher publisher = new MessagePublisher();
        publisher.subscribe(squareQueue);
        publisher.subscribe(waterfallQueue);
        publisher.subscribe(lineQueue);

        Simulation simulation = new Simulation(targetCount, publisher);

        // ---- 3) iki OpenGL canvas ----
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities caps = new GLCapabilities(profile);

        RadarCanvas    squareCanvas    = new RadarCanvas(caps, squareQueue);
        WaterfallCanvas waterfallCanvas = new WaterfallCanvas(caps, waterfallQueue);
        LineCanvas     lineCanvas      = new LineCanvas(caps, lineQueue);

        // ---- 4) CardLayout ile ikisini ust uste koy, menuden sec ----
        CardLayout cards = new CardLayout();
        JPanel center = new JPanel(cards);
        center.setPreferredSize(new Dimension(1000, 1000));
        center.add(squareCanvas, CARD_SQUARE);
        center.add(waterfallCanvas, CARD_WATERFALL);
        center.add(lineCanvas, CARD_LINE);

        JFrame frame = new JFrame("Radar - Square / Waterfall");
        frame.setJMenuBar(buildMenuBar(cards, center));
        frame.getContentPane().add(center);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        FPSAnimator squareAnim    = new FPSAnimator(squareCanvas, FPS, true);
        FPSAnimator waterfallAnim = new FPSAnimator(waterfallCanvas, FPS, true);
        FPSAnimator lineAnim      = new FPSAnimator(lineCanvas, FPS, true);

        // ---- 5) kapatirken duzgun durdur ----
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                new Thread(() -> {
                    simulation.stop();
                    squareCanvas.stopConsuming();
                    waterfallCanvas.stopConsuming();
                    lineCanvas.stopConsuming();
                    squareAnim.stop();
                    waterfallAnim.stop();
                    lineAnim.stop();
                }).start();
            }
        });

        frame.setVisible(true);
        cards.show(center, CARD_SQUARE);   // acilista square

        // ---- 6) her sey calissin ----
        squareAnim.start();
        waterfallAnim.start();
        lineAnim.start();
        squareCanvas.startConsuming();
        waterfallCanvas.startConsuming();
        lineCanvas.startConsuming();
        simulation.start();
    }

    // hedef sayisini soran ufak baslangic ekrani
    private static int askTargetCount() {
        String input = JOptionPane.showInputDialog(
                null,
                "Hedef sayisi:",
                "Baslangic",
                JOptionPane.QUESTION_MESSAGE);

        if (input == null) {
            return -1;   // iptal / kapatma
        }
        try {
            return Math.max(0, Integer.parseInt(input.trim()));
        } catch (NumberFormatException ex) {
            return DEFAULT_TARGET_COUNT;   // gecersiz giris -> varsayilan
        }
    }

    // ustte Square / Waterfall secim menusu
    private static JMenuBar buildMenuBar(CardLayout cards, JPanel center) {
        JMenuBar bar = new JMenuBar();
        JMenu menu = new JMenu("Grafik");

        JRadioButtonMenuItem square    = new JRadioButtonMenuItem("Square", true);
        JRadioButtonMenuItem waterfall = new JRadioButtonMenuItem("Waterfall");
        JRadioButtonMenuItem line      = new JRadioButtonMenuItem("Line");

        ButtonGroup group = new ButtonGroup();
        group.add(square);
        group.add(waterfall);
        group.add(line);

        square.addActionListener(e    -> cards.show(center, CARD_SQUARE));
        waterfall.addActionListener(e -> cards.show(center, CARD_WATERFALL));
        line.addActionListener(e      -> cards.show(center, CARD_LINE));

        menu.add(square);
        menu.add(waterfall);
        menu.add(line);
        bar.add(menu);
        return bar;
    }
}
