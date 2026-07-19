package com.radar.app;

import com.radar.factory.GraphFactory.GraphType;

/**
 * Uygulamanın giriş noktası.
 *
 * <p>Kütüphane kullanım örneği — mentörün istediği tek satırlık çağrı:</p>
 * <pre>{@code
 * RadarLibrary.create()
 *     .withShipCount(50)
 *     .withFrequency(60)
 *     .withRadarSpeed(80.0)
 *     .withGraph(GraphType.RADAR)
 *     .start();
 * }</pre>
 */
public class Main {

    public static void main(String[] args) {
        // Look and Feel ayarla
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Varsayilan ile devam et
        }

        com.radar.ui.StartupDialog dialog = new com.radar.ui.StartupDialog();
        dialog.setVisible(true);

        if (dialog.isStartSimulation()) {
            RadarLibrary.create()
                    .withShipCount(dialog.getShipCount())
                    .withFrequency(dialog.getFps())
                    .withRadarSpeed(dialog.getRadarSpeed())
                    .withGraph(dialog.getSelectedGraph())
                    .start();
        } else {
            System.out.println("Simülasyon başlatılmadan çıkıldı.");
            System.exit(0);
        }
    }
}
