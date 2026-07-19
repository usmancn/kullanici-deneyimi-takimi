package com.radar.graphs;

/**
 * Her grafik bileseninin uygulayacagi kontrat.
 * Factory Pattern ve yeni grafik eklemesini kolaylastirir.
 *
 * <p>Implementasyonlar bir Swing {@link java.awt.Component} olmalidir
 * (JPanel veya GLCanvas) ve MainFrame'deki sekmeye dogrudan eklenebilir.</p>
 */
public interface IGraph {

    /**
     * Grafigi baslatir / animasyonu baslatir.
     * Sekme gorune geldiginde cagirilir.
     */
    void startGraph();

    /**
     * Grafigi durdurur / animasyonu durdurur.
     * Sekme gizlendiginde veya pencere kapanirken cagirilir.
     */
    void stopGraph();
}
