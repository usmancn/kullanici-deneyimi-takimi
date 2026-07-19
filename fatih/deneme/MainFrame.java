package deneme;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JFrame;

public class MainFrame extends JFrame{
	
	private SimulationOptions simulationOptions;
	private DebugTable debugTable;
	private FirstGraph firstGraph;
	
	public MainFrame() {
		super("Graphical Interface");
		
		setLayout(new GridBagLayout());
		GridBagConstraints griddy = new GridBagConstraints();
		simulationOptions = new SimulationOptions();
		debugTable = new DebugTable();
		firstGraph = new FirstGraph(0,0);
		
		simulationOptions.setIDebugWritter(new IDebugWritter() {
			public void WriteDebug(String msg) {
				debugTable.addLog(msg);
			}
		});
		simulationOptions.setIFPSController(new IFPSController() {
			public void setFPS(int frequency) {
				firstGraph.executeGraph(frequency);
			}		
		});
		
		//_________SIMULATION OPTIONS___________
		
		griddy.gridx = 0;
		griddy.gridy = 0;
		griddy.gridwidth = 3;
		griddy.gridheight = 2;
		griddy.weightx = 1.0;
		griddy.weighty = 1.0;
		griddy.anchor = GridBagConstraints.CENTER;
		griddy.fill = GridBagConstraints.HORIZONTAL; 
		add(simulationOptions,griddy);
		
		
		//_________DEBUG TABLE___________
		griddy.gridy = 2;
		griddy.gridheight = 8;
		add(debugTable,griddy);
			
		
		setSize(1000,1000);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}
	
}
