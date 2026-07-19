package deneme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

public class DebugTable extends JPanel {

    private static final Color HEADER_BG  = new Color(218, 165, 32);
    private static final Color ERROR_BG   = new Color(178, 34, 34);
    private static final Color START_BG   = new Color(34, 139, 34);
    private static final Color STOP_BG    = new Color(178, 34, 34);
    private static final Color GRAPH_BG   = new Color(25, 42, 86);
    private static final Color DEFAULT_BG = Color.WHITE;
    private static final Color DEFAULT_FG = Color.BLACK;

    private JTable table;
    private DebugTableModel debugTableModel;

    public DebugTable() {
        setLayout(new BorderLayout());
        debugTableModel = new DebugTableModel();
        table = new JTable(debugTableModel);

        table.setRowHeight(24);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setDefaultRenderer(Object.class, new DebugCellRenderer());

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getWidth(), 28));
        header.setDefaultRenderer(new HeaderRenderer());

        add(new JScrollPane(table), BorderLayout.CENTER);
        setPreferredSize(new Dimension(1000, 800));
    }

    public void addLog(String msg) {
        debugTableModel.addRow(msg);
    }
    private static class HeaderRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            setBackground(HEADER_BG);
            setForeground(Color.WHITE);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(getFont().deriveFont(Font.BOLD));
            setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return this;
        }
    }

    private static class DebugCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(t, value, false, false, row, column);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < t.getColumnCount(); i++) {
                Object o = t.getValueAt(row, i);
                if (o != null) sb.append(o.toString()).append(' ');
            }
            String line = sb.toString().toLowerCase();

            Color bg = DEFAULT_BG;
            Color fg = DEFAULT_FG;

            if (line.contains("hata")) {
                bg = ERROR_BG;
                fg = Color.WHITE;
            } else if (line.contains("simulation started")) {
                bg = START_BG;
                fg = Color.WHITE;
            } else if (line.contains("simulation stopped")) {
                bg = STOP_BG;
                fg = Color.WHITE;
            } else if (line.contains("opened")) {
                bg = GRAPH_BG;
                fg = Color.WHITE;
            }

            if (isSelected) {
                bg = bg.darker();
            }

            setBackground(bg);
            setForeground(fg);
            return this;
        }
    }
}
