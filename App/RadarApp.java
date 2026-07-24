package deneme.App;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import deneme.Controller.ApplicationController;
import deneme.Detection.ObjectDetector;
import deneme.Graph.IRadarCanvas;
import deneme.Simulation.Simulation;

/**
 * Radar uygulamasinin gorunumu: birden fazla canvas'i (kart olarak) tutan
 * pencere, grafik secim menusu, opsiyonel gain slider. Yasam dongusunu
 * (baslat/durdur/pencere kapanisi) {@link ApplicationController} yonetir;
 * start()/stop() ona delege eder. AppBuilder uretir; en az bir kart ve
 * simulasyon zorunludur.
 */
public class RadarApp {

	/** Menude bir secenek olarak gorunen tek kart: isim + icerik + icindeki canvas'lar. */
	public static final class Card {
		private final String name;
		private final Component component;
		private final List<GLCanvas> canvases;

		public Card(String name, Component component, List<GLCanvas> canvases) {
			this.name = name;
			this.component = component;
			this.canvases = new ArrayList<>(canvases);
		}

		/** Tek canvas'lik kart icin kisayol. */
		public Card(String name, GLCanvas canvas) {
			this(name, canvas, java.util.Collections.singletonList(canvas));
		}

		public String getName() { return name; }
	}

	private final List<Card> cards;
	private final GainFilterSlider gainSlider;  // null: slider yok

	private final JFrame frame;
	private final CardLayout cardLayout = new CardLayout();
	private final JPanel center = new JPanel(cardLayout);
	private final List<FPSAnimator> animators = new ArrayList<>();
	private final ApplicationController controller;

	public RadarApp(String title, Simulation simulation, ObjectDetector detector,
	                List<Card> cards, GainFilterSlider gainSlider,
	                SliderPosition sliderPosition, int animatorFps) {
		if (cards == null || cards.isEmpty()) {
			throw new IllegalStateException("En az bir canvas gerekli");
		}
		if (simulation == null) {
			throw new IllegalStateException("Simulasyon zorunlu");
		}
		this.cards = new ArrayList<>(cards);
		this.gainSlider = gainSlider;

		center.setPreferredSize(new Dimension(1000, 1000));
		for (Card card : this.cards) {
			center.add(card.component, card.name);
			for (GLCanvas canvas : card.canvases) {
				animators.add(new FPSAnimator(canvas, animatorFps, true));
			}
		}

		frame = new JFrame(title);
		if (this.cards.size() > 1) {
			frame.setJMenuBar(buildMenuBar());
		}
		frame.getContentPane().add(center, BorderLayout.CENTER);
		if (gainSlider != null) {
			String edge = (sliderPosition == SliderPosition.TOP) ? BorderLayout.NORTH
			                                                     : BorderLayout.SOUTH;
			frame.getContentPane().add(gainSlider, edge);
		}
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// ---- yasam dongusu controller'a devredilir (pencere kapanisi dahil) ----
		this.controller = new ApplicationController(simulation, detector, this);
		this.controller.install();
	}

	/** Pencereyi acar, animator/consumer/dedektor/simulasyonu calistirir. */
	public void start() { controller.start(); }

	/** Her seyi durdurur (pencere kapanirken controller da cagirir). */
	public void stop() { controller.stop(); }

	public JFrame getFrame() { return frame; }

	// ---- ApplicationController'in kullandigi gorunum islemleri ----

	/** Acilista ilk karti gosterir ve odagi ona verir. */
	public void showFirstCard() {
		cardLayout.show(center, cards.get(0).name);
		focusVisibleCard();
	}

	public void startAnimators() {
		for (FPSAnimator animator : animators) {
			animator.start();
		}
	}

	public void stopAnimators() {
		for (FPSAnimator animator : animators) {
			animator.stop();
		}
	}

	/** Tum kartlardaki canvas'larin veri tuketimini baslatir. */
	public void startConsuming() {
		for (Card card : cards) {
			for (GLCanvas canvas : card.canvases) {
				((IRadarCanvas) canvas).startConsuming();
			}
		}
	}

	/** Tum kartlardaki canvas'larin veri tuketimini durdurur. */
	public void stopConsuming() {
		for (Card card : cards) {
			for (GLCanvas canvas : card.canvases) {
				((IRadarCanvas) canvas).stopConsuming();
			}
		}
	}

	// secilen kart gorunur olunca odagi ona ver (TAB ile minimap ac/kapa icin)
	private void focusVisibleCard() {
		for (Component c : center.getComponents()) {
			if (c.isVisible()) {
				c.requestFocusInWindow();
				return;
			}
		}
	}

	// ustte grafik secim menusu: sadece mevcut kartlar listelenir
	private JMenuBar buildMenuBar() {
		JMenuBar bar = new JMenuBar();
		JMenu menu = new JMenu("Grafik");
		ButtonGroup group = new ButtonGroup();

		boolean first = true;
		for (Card card : cards) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(card.name, first);
			first = false;
			item.addActionListener(e -> {
				cardLayout.show(center, card.name);
				focusVisibleCard();
			});
			group.add(item);
			menu.add(item);
		}

		bar.add(menu);
		return bar;
	}
}
