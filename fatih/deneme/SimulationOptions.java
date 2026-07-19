package deneme;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import deneme.radar.RadarView;   // YENI: parcalanmis radar + kayan bar (gain filtresi)

public class SimulationOptions extends JPanel{

	private static final Color GREEN = new Color(34, 139, 34);
	private static final Color RED   = new Color(178, 34, 34);
	private static final Color NAVY  = new Color(25, 42, 86);

	private IDebugWritter debugger;
	private IFPSController fpsController;

	public void setIDebugWritter(IDebugWritter debugger) {
		this.debugger = debugger;
	}
	public void setIFPSController(IFPSController fpsController) {
		this.fpsController = fpsController;
	}


	//_________HZ OPTION___________
	private JLabel label1;
	private JTextField text1;

	//_________NUMBER OF ENEMY SHIPS___________
	private JLabel label2;
	private JTextField text2;

	//_________BUTTONS__________
	private JButton btn1;
	private JButton btn2;
	private JButton btn3;
	private JButton btn4;
	private JButton btn5;


	public SimulationOptions() {

		setLayout(new GridBagLayout());
		GridBagConstraints griddy = new GridBagConstraints();

		label1 = new JLabel("Hz: ");
		label2 = new JLabel("Number Of Ships: ");
		text1 = new JTextField(2);
		text2 = new JTextField(2);
		btn1 = new RoundButton("Start the simulation", GREEN);
		btn2 = new RoundButton("Stop the simulation", RED);
		btn3 = new RoundButton("Graph-1", NAVY);
		btn4 = new RoundButton("Graph-2", NAVY);
		btn5 = new RoundButton("Graph-3", NAVY);


		btn1.setEnabled(true);
		btn2.setEnabled(false); 
		btn3.setEnabled(false);
		btn4.setEnabled(false);
		btn5.setEnabled(false);

		btn1.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {

		        int frequency;
		        int enemyCount;

		        try {
		            frequency = Integer.parseInt(text1.getText().trim());
		        } catch (NumberFormatException ex) {
		            if (debugger != null)
		                debugger.WriteDebug("HATA: Frekans geçerli bir sayı değil: '" + text1.getText() + "'");
		            text1.requestFocus();
		            return;                      
		        }
		        if (frequency <= 0) {
		            if (debugger != null)
		                debugger.WriteDebug("HATA: Frekans pozitif olmalı.");
		            return;
		        }

		        try {
		            enemyCount = Integer.parseInt(text2.getText().trim());
		        } catch (NumberFormatException ex) {
		            if (debugger != null)
		                debugger.WriteDebug("HATA: Düşman sayısı tam sayı değil: '" + text2.getText() + "'");
		            text2.requestFocus();
		            return;
		        }
		        if (enemyCount <= 0) {
		            if (debugger != null)
		                debugger.WriteDebug("HATA: Gemi sayısı pozitif olmalı.");
		            return;
		        }

		        if (debugger != null)
		            debugger.WriteDebug("Simulation started " + frequency + " Hz with " + enemyCount + " Enemy Ships");

		        if (fpsController != null) {
		        	fpsController.setFPS(frequency);
		        }

		        btn1.setEnabled(false);
		        btn2.setEnabled(true);
		        btn3.setEnabled(true);
		        btn4.setEnabled(true);
		        btn5.setEnabled(true);
		        text1.setEnabled(false);
		        text2.setEnabled(false);
		    }
		});

		btn2.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if(debugger != null)
					debugger.WriteDebug("Simulation stopped. ");
				btn1.setEnabled(true);
				btn2.setEnabled(false);
				btn3.setEnabled(false);
				btn4.setEnabled(false);
				btn5.setEnabled(false);
				text2.setEnabled(true);
				text1.setEnabled(true);
			}
		});

		//_________GRAPH-1 : YENI PARCALANMIS RADAR + GAIN FILTRESI___________
		btn3.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {

		        int frequency, count;
		        try {
		            frequency = Integer.parseInt(text1.getText().trim());
		        } catch (NumberFormatException ex) {
		            if (debugger != null)
		                debugger.WriteDebug("HATA: Frekans geçerli bir tam sayı değil: '" + text1.getText() + "'");
		            return;
		        }
		        try {
		            count = Integer.parseInt(text2.getText().trim());
		        } catch (NumberFormatException ex) {
		            if (debugger != null)
		                debugger.WriteDebug("HATA: Gemi sayısı geçerli bir tam sayı değil: '" + text2.getText() + "'");
		            return;
		        }

		        JFrame graphFrame = new JFrame("Radar");
		        RadarView view = new RadarView(frequency, count);   // radar + alttaki kayan bar

		        graphFrame.add(view);
		        graphFrame.pack();
		        graphFrame.setLocationRelativeTo(null);
		        graphFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		        graphFrame.setVisible(true);
		        if (debugger != null)
		            debugger.WriteDebug("Radar açıldı (" + frequency + " Hz, " + count + " gemi)");
		    }
		});

        btn4.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				int frequency, count;
		        try {
		            frequency = Integer.parseInt(text1.getText().trim());
		        } catch (NumberFormatException ex) {
		            if (debugger != null)
		                debugger.WriteDebug("HATA: Frekans geçerli bir tam sayı değil: '" + text1.getText() + "'");
		            return;
		        }
		        try {
		            count = Integer.parseInt(text2.getText().trim());
		        } catch (NumberFormatException ex) {
		            if (debugger != null)
		                debugger.WriteDebug("HATA: Gemi sayısı geçerli bir tam sayı değil: '" + text2.getText() + "'");
		            return;
		        }

		        JFrame graphFrame = new JFrame("Graph-1");
		        SecondGraph graph = new SecondGraph(count, frequency);

		        graphFrame.add(graph);
		        graphFrame.pack();
		        graphFrame.setLocationRelativeTo(null);
		        graphFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); 


		        graphFrame.setVisible(true);
		        if (debugger != null)
		            debugger.WriteDebug("Graph-1 Opened (" + frequency + " Hz)");
			}
		});

        btn5.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if(debugger != null)
					debugger.WriteDebug("Graph-3 Opened");
			}
		});

		//_________LABEL1___________

		griddy.gridx = 0;
		griddy.gridy = 0;
		griddy.gridwidth = 1;
		griddy.gridheight = 1;
		griddy.weightx = 1.0;
		griddy.weighty = 1.0;

		griddy.ipadx = 20;
		griddy.ipady = 20;
		griddy.insets = new Insets(10, 8, 10, 20);
		add(label1,griddy);

		//_________TEXT1___________

		griddy.anchor = GridBagConstraints.LINE_START;
		griddy.insets = new Insets(10, 0, 10, 8);
		griddy.gridx = 1;
		add(text1,griddy);

		//_________LABEL2___________

		griddy.anchor = GridBagConstraints.LINE_END;
		griddy.insets = new Insets(10, 8, 10, 20);
		griddy.gridx = 2;
		add(label2,griddy);

		//_________TEXT2___________

		griddy.anchor = GridBagConstraints.LINE_START;
		griddy.insets = new Insets(10, 0, 10, 8);
		griddy.gridx = 3;
		add(text2,griddy);

		//_________BUTTON1___________

		griddy.anchor = GridBagConstraints.CENTER;
		griddy.insets = new Insets(10, 8, 10, 8);
		griddy.gridx = 4;
		add(btn1,griddy);

		//_________BUTTON2___________

		griddy.gridx = 5;
		add(btn2,griddy);

		//_________BUTTON3___________

		griddy.gridy = 1;
        griddy.gridwidth = 2;
		griddy.gridheight = 1;
        griddy.weightx = 1.0;
        griddy.anchor = GridBagConstraints.CENTER;
        griddy.ipadx = 20;
        griddy.ipady = 20;
        griddy.insets = new Insets(18, 8, 18, 8);

        griddy.gridx = 0; 
        add(btn3, griddy);

        //_________BUTTON4___________

        griddy.gridx = 2; 
        add(btn4, griddy);

        //_________BUTTON5___________

        griddy.gridx = 4; 
        add(btn5, griddy);

	}

	private static class RoundButton extends JButton {

		private final Color base;

		public RoundButton(String text, Color base) {
			super(text);
			this.base = base;
			setForeground(Color.WHITE);
			setFocusPainted(false);
			setBorderPainted(false);
			setContentAreaFilled(false);
			setOpaque(false);
			setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
		}

		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			Color fill;
			if (!isEnabled()) {
				fill = new Color(base.getRed(), base.getGreen(), base.getBlue(), 90);
			} else if (getModel().isPressed()) {
				fill = base.darker();
			} else if (getModel().isRollover()) {
				fill = base.brighter();
			} else {
				fill = base;
			}

			g2.setColor(fill);
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
			g2.dispose();

			super.paintComponent(g);
		}
	}
}