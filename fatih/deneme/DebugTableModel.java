package deneme;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;


public class DebugTableModel extends AbstractTableModel {

    private final String[] columnNames = { "ID", "Mesaj", "Zaman" };
    private final List<Message> rows = new ArrayList<>();

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        Message msg = rows.get(row);
        switch (col) {
            case 0:  return msg.getId();
            case 1:  return msg.getMessage();
            case 2:  return msg.getTime();
            default: return null;
        }
    }

    public void addRow(String message) {
        rows.add(new Message(message));
        int index = rows.size() - 1;  
        fireTableRowsInserted(index, index);
    }

    public void removeRow(int index) {
        rows.remove(index);
        fireTableRowsDeleted(index, index);
    }

}
