package com.radar.app;


/**
 * Uygulamanın giriş noktası.
 *
 * Kütüphane kullanım örneği:
 * RadarLibrary.create().withShipCount(50)...start();
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
