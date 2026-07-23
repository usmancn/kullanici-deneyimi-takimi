package deneme.App;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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

import deneme.Detection.ObjectDetector;
import deneme.MessageProcess.MessagePublisher;
import deneme.Simulation.Simulation;

public class Main{

    private static final int FPS = 40;
    private static final int DEFAULT_TARGET_COUNT = 15;

    private static final String CARD_SQUARE = "square";
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
        RadarQueues queues = new RadarQueues();

        MessagePublisher publisher = new MessagePublisher();
        queues.subscribeAll(publisher);

        Simulation simulation = new Simulation(targetCount, publisher);

        // scanline tabanli obje dedektoru: obje bulunca Simulation'a ID sorar
        ObjectDetector detector = new ObjectDetector(queues.detection, simulation);
        
        // heavyweight popup: GLCanvas uzerinde hafif popup'lar gorunmez
        javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        // ---- 3) iki OpenGL canvas ----
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities caps = new GLCapabilities(profile);
        
        GraphBundle graphs = new GraphBundle(caps, queues, FPS);


        // sag tik menusu: mark / change mark / unmark
        graphs.square.installTargetMarkController(simulation);
        graphs.circular.installTargetMarkController(simulation);

        // line + waterfall tek kartta, alt alta: her biri 1000x500 yarida durur,
        // Viewport kisa kenara gore kare aldigi icin ikisi de 500x500 cizer
        JPanel lineWaterfall = new JPanel(new GridLayout(2, 1));
        lineWaterfall.add(graphs.line);
        lineWaterfall.add(graphs.waterfall);

        // ---- 4) CardLayout ile ikisini ust uste koy, menuden sec ----
        CardLayout cards = new CardLayout();
        JPanel center = new JPanel(cards);
        center.setPreferredSize(new Dimension(1000, 1000));
        center.add(graphs.square, CARD_SQUARE);
        center.add(lineWaterfall, CARD_LINE);
        center.add(graphs.circular, CARD_CIRCULAR);

        GainFilterSlider gainSlider = new GainFilterSlider();

        JFrame frame = new JFrame("Radar - Square / Waterfall");
        frame.setJMenuBar(buildMenuBar(cards, center, gainSlider));
        frame.getContentPane().add(center, BorderLayout.CENTER);
        frame.getContentPane().add(gainSlider, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        
        // ---- 5) kapatirken duzgun durdur ----
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                new Thread(() -> {
                    simulation.stop();
                    graphs.stopGraphs();
                    detector.stop();
                    graphs.stopAnimators();
                }).start();
            }
        });

        frame.setVisible(true);
        cards.show(center, CARD_SQUARE);   // acilista square
        graphs.square.requestFocusInWindow();   // TAB (minimap) icin odak canvas'ta olsun

        // ---- 6) her sey calissin ----
        
        graphs.startAnimators();   
        graphs.startGraphs();
        
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
        JRadioButtonMenuItem line = new JRadioButtonMenuItem("Line + Waterfall");
        JRadioButtonMenuItem circular = new JRadioButtonMenuItem("Circular");

        ButtonGroup group = new ButtonGroup();
        group.add(square);
        group.add(line);
        group.add(circular);

        square.addActionListener(e    -> { cards.show(center, CARD_SQUARE);   gainSlider.setVisible(true); focusCard(center); });
        line.addActionListener(e      -> { cards.show(center, CARD_LINE);     gainSlider.setVisible(true); focusCard(center); });
        circular.addActionListener(e  -> { cards.show(center, CARD_CIRCULAR); gainSlider.setVisible(true); focusCard(center); });

        menu.add(square);
        menu.add(line);
        menu.add(circular);
        bar.add(menu);
        return bar;
    }
}