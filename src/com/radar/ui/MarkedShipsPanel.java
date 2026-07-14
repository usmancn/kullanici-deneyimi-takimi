package com.radar.ui;

import com.radar.engine.EntityManager;
import com.radar.model.Ship;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

public class MarkedShipsPanel extends JPanel {

    private final EntityManager entityManager;
    private final MarkedShipsTableModel tableModel;
    private final JTable table;
    private final Timer updateTimer;

    public MarkedShipsPanel(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.setLayout(new BorderLayout());
        this.setBackground(new Color(14, 14, 20));

        // Üst bilgi paneli
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(new Color(14, 14, 20));
        JLabel title = new JLabel("İşaretlenmiş Gemiler");
        title.setForeground(new Color(200, 60, 60));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14.0f));
        topPanel.add(title);
        
        JLabel info = new JLabel(" (Özel İsim sütununa çift tıklayarak isim verebilirsiniz)");
        info.setForeground(new Color(150, 150, 150));
        info.setFont(info.getFont().deriveFont(11.0f));
        topPanel.add(info);
        
        add(topPanel, BorderLayout.NORTH);

        // Tablo modeli ve tablo
        tableModel = new MarkedShipsTableModel();
        table = new JTable(tableModel);
        
        // Görünüm ayarları
        table.setBackground(new Color(20, 20, 28));
        table.setForeground(new Color(220, 220, 220));
        table.setGridColor(new Color(60, 60, 80));
        table.getTableHeader().setBackground(new Color(30, 30, 40));
        table.getTableHeader().setForeground(new Color(200, 200, 200));
        table.setRowHeight(24);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(new Color(14, 14, 20));
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(scrollPane, BorderLayout.CENTER);

        // Güncelleme timer'ı (saniyede 2 kez güncelle)
        updateTimer = new Timer(500, e -> updateData());
        updateTimer.start();
    }

    private void updateData() {
        if (table.isEditing()) {
            return; // Düzenleme yapılırken listeyi yenileme (yoksa edit kapanır)
        }
        tableModel.setShips(entityManager.getMarkedShips());
    }

    private static class MarkedShipsTableModel extends AbstractTableModel {
        
        private final String[] columnNames = {"ID (Kısa)", "Özel İsim", "X", "Y"};
        private List<Ship> ships = new ArrayList<>();

        public void setShips(List<Ship> ships) {
            this.ships = ships;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return ships.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return columnNames[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1; // Sadece Özel İsim değiştirilebilir
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= ships.size()) return "";
            Ship ship = ships.get(rowIndex);
            
            switch (columnIndex) {
                case 0:
                    UUID id = ship.getId();
                    return id.toString().substring(0, 8);
                case 1:
                    return ship.getCustomName();
                case 2:
                    return String.format("%d", (int) ship.getPosition().x);
                case 3:
                    return String.format("%d", (int) ship.getPosition().y);
                default:
                    return "";
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 1 && rowIndex < ships.size()) {
                Ship ship = ships.get(rowIndex);
                String newName = aValue.toString().trim();
                if (!newName.isEmpty()) {
                    ship.setCustomName(newName);
                }
            }
        }
    }
}
