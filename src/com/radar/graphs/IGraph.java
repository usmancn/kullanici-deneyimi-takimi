package com.radar.graphs;

/**
 * Her grafik bileseninin uygulayacagi kontrat.
 * Factory Pattern ve yeni grafik eklemesini kolaylastirir.
 *
 * Implementasyonlar bir Swing Component olmalidir. Frame'deki sekmeye dogrudan eklenebilir.
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
