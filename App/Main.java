package deneme.App;

import java.awt.BorderLayout;
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

import deneme.Detection.ObjectDetector;
import deneme.Graph.Circular.CircularCanvas;
import deneme.Graph.Line.LineCanvas;
import deneme.Graph.Square.SquareCanvas;
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
    private static final String CARD_CIRCULAR = "circular";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        // ---- 1) baslangic ekrani: hedef sayisi ----
        int targetCount = askTargetCount();
        if (targetCount < 0) {
            return; // kullanici iptal etti
        }

        // ---- 2) mesaj hatti: Simulation -> (square queue, waterfall queue) ----
        BlockingQueue<QueueMessage> squareQueue = new LinkedBlockingQueue<>();
        BlockingQueue<QueueMessage> waterfallQueue = new LinkedBlockingQueue<>();
        BlockingQueue<QueueMessage> lineQueue = new LinkedBlockingQueue<>();
        BlockingQueue<QueueMessage> circularQueue = new LinkedBlockingQueue<>();
        BlockingQueue<QueueMessage> detectionQueue = new LinkedBlockingQueue<>();

        MessagePublisher publisher = new MessagePublisher();
        publisher.subscribe(squareQueue);
        publisher.subscribe(waterfallQueue);
        publisher.subscribe(lineQueue);
        publisher.subscribe(circularQueue);
        publisher.subscribe(detectionQueue);

        Simulation simulation = new Simulation(targetCount, publisher);

        // scanline tabanli obje dedektoru: obje bulunca Simulation'a ID sorar
        ObjectDetector detector = new ObjectDetector(detectionQueue, simulation);

        // ---- 3) iki OpenGL canvas ----
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities caps = new GLCapabilities(profile);

        SquareCanvas squareCanvas = new SquareCanvas(caps, squareQueue);
        WaterfallCanvas waterfallCanvas = new WaterfallCanvas(caps, waterfallQueue);
        LineCanvas lineCanvas = new LineCanvas(caps, lineQueue);
        CircularCanvas circularCanvas = new CircularCanvas(caps, circularQueue);

        // ---- 4) CardLayout ile ikisini ust uste koy, menuden sec ----
        CardLayout cards = new CardLayout();
        JPanel center = new JPanel(cards);
        center.setPreferredSize(new Dimension(1000, 1000));
        center.add(squareCanvas, CARD_SQUARE);
        center.add(waterfallCanvas, CARD_WATERFALL);
        center.add(lineCanvas, CARD_LINE);
        center.add(circularCanvas, CARD_CIRCULAR);

        GainFilterSlider gainSlider = new GainFilterSlider();

        JFrame frame = new JFrame("Radar - Square / Waterfall");
        frame.setJMenuBar(buildMenuBar(cards, center, gainSlider));
        frame.getContentPane().add(center, BorderLayout.CENTER);
        frame.getContentPane().add(gainSlider, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        FPSAnimator squareAnim = new FPSAnimator(squareCanvas, FPS, true);
        FPSAnimator waterfallAnim = new FPSAnimator(waterfallCanvas, FPS, true);
        FPSAnimator lineAnim = new FPSAnimator(lineCanvas, FPS, true);
        FPSAnimator circularAnim = new FPSAnimator(circularCanvas, FPS, true);

        // ---- 5) kapatirken duzgun durdur ----
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                new Thread(() -> {
                    simulation.stop();
                    squareCanvas.stopConsuming();
                    waterfallCanvas.stopConsuming();
                    lineCanvas.stopConsuming();
                    circularCanvas.stopConsuming();
                    detector.stop();
                    squareAnim.stop();
                    waterfallAnim.stop();
                    lineAnim.stop();
                    circularAnim.stop();
                }).start();
            }
        });

        frame.setVisible(true);
        cards.show(center, CARD_SQUARE); // acilista square
        cards.show(center, CARD_SQUARE);   // acilista square
        squareCanvas.requestFocusInWindow();   // TAB (minimap) icin odak canvas'ta olsun

        // ---- 6) her sey calissin ----
        squareAnim.start();
        waterfallAnim.start();
        lineAnim.start();
        circularAnim.start();
        squareCanvas.startConsuming();
        waterfallCanvas.startConsuming();
        lineCanvas.startConsuming();
        circularCanvas.startConsuming();
        detector.start();
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
            return -1; // iptal / kapatma
        }
        try {
            return Math.max(0, Integer.parseInt(input.trim()));
        } catch (NumberFormatException ex) {
            return DEFAULT_TARGET_COUNT; // gecersiz giris -> varsayilan
        }
    }

    // secilen kart gorunur olunca odagi ona ver (TAB ile minimap ac/kapa icin)
    private static void focusCard(JPanel center) {
        for (java.awt.Component c : center.getComponents()) {
            if (c.isVisible()) {
                c.requestFocusInWindow();
                return;
            }
        }
    }

    // ustte Square / Waterfall secim menusu
    private static JMenuBar buildMenuBar(CardLayout cards, JPanel center, GainFilterSlider gainSlider) {
        JMenuBar bar = new JMenuBar();
        JMenu menu = new JMenu("Grafik");

        JRadioButtonMenuItem square = new JRadioButtonMenuItem("Square", true);
        JRadioButtonMenuItem waterfall = new JRadioButtonMenuItem("Waterfall");
        JRadioButtonMenuItem line = new JRadioButtonMenuItem("Line");
        JRadioButtonMenuItem circular = new JRadioButtonMenuItem("Circular");

        ButtonGroup group = new ButtonGroup();
        group.add(square);
        group.add(waterfall);
        group.add(line);
        group.add(circular);

        // gain filtresi waterfall disindaki grafiklerde gorunur
        square.addActionListener(e -> {
            cards.show(center, CARD_SQUARE);
            gainSlider.setVisible(true);
        });
        waterfall.addActionListener(e -> {
            cards.show(center, CARD_WATERFALL);
            gainSlider.setVisible(false);
        });
        line.addActionListener(e -> {
            cards.show(center, CARD_LINE);
            gainSlider.setVisible(true);
        });
        circular.addActionListener(e -> {
            cards.show(center, CARD_CIRCULAR);
            gainSlider.setVisible(true);
        });
        square.addActionListener(e    -> { cards.show(center, CARD_SQUARE);    gainSlider.setVisible(true);  focusCard(center); });
        waterfall.addActionListener(e -> { cards.show(center, CARD_WATERFALL); gainSlider.setVisible(false); focusCard(center); });
        line.addActionListener(e      -> { cards.show(center, CARD_LINE);      gainSlider.setVisible(true);  focusCard(center); });
        circular.addActionListener(e  -> { cards.show(center, CARD_CIRCULAR);  gainSlider.setVisible(true);  focusCard(center); });

        menu.add(square);
        menu.add(waterfall);
        menu.add(line);
        menu.add(circular);
        bar.add(menu);
        return bar;
    }
}
