package deneme.App;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

public class RadarFrame extends JFrame {

    private static final String CARD_SQUARE = "square";
    private static final String CARD_LINE = "line";
    private static final String CARD_CIRCULAR = "circular";

    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);

    public RadarFrame(GraphBundle graphs, GainFilterSlider gainSlider) {
        super("Radar - Square / Waterfall");

        JPanel lineWaterfall = new JPanel(new GridLayout(2, 1));
        lineWaterfall.add(graphs.line);
        lineWaterfall.add(graphs.waterfall);

        center.setPreferredSize(new Dimension(1000, 1000));
        center.add(graphs.square, CARD_SQUARE);
        center.add(lineWaterfall, CARD_LINE);
        center.add(graphs.circular, CARD_CIRCULAR);

        setJMenuBar(buildMenuBar(gainSlider));
        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(gainSlider, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        showSquare();
    }

    public void showSquare() {
        cards.show(center, CARD_SQUARE);
    }

    private void focusCurrentCard() {
        for (java.awt.Component component : center.getComponents()) {
            if (component.isVisible()) {
                component.requestFocusInWindow();
                return;
            }
        }
    }

    private JMenuBar buildMenuBar(GainFilterSlider gainSlider) {
        JMenuBar bar = new JMenuBar();
        JMenu menu = new JMenu("Grafik");

        JRadioButtonMenuItem square = new JRadioButtonMenuItem("Square", true);
        JRadioButtonMenuItem line = new JRadioButtonMenuItem("Line + Waterfall");
        JRadioButtonMenuItem circular = new JRadioButtonMenuItem("Circular");

        ButtonGroup group = new ButtonGroup();
        group.add(square);
        group.add(line);
        group.add(circular);

        square.addActionListener(e -> {
            cards.show(center, CARD_SQUARE);
            gainSlider.setVisible(true);
            focusCurrentCard();
        });

        line.addActionListener(e -> {
            cards.show(center, CARD_LINE);
            gainSlider.setVisible(true);
            focusCurrentCard();
        });

        circular.addActionListener(e -> {
            cards.show(center, CARD_CIRCULAR);
            gainSlider.setVisible(true);
            focusCurrentCard();
        });

        menu.add(square);
        menu.add(line);
        menu.add(circular);
        bar.add(menu);

        return bar;
    }
}