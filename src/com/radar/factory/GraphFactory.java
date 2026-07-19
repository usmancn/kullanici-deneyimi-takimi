package com.radar.factory;

import com.radar.config.SimulationConfig;
import com.radar.sim.engine.EntityManager;
import com.radar.graphs.CircularGraph;
import com.radar.graphs.IGraph;
import com.radar.graphs.LineGraph;
import com.radar.graphs.WaterfallGraph;
import com.radar.gl.ui.RadarCanvas;

import java.awt.Component;

/**
 * Factory Design Pattern: grafik bilesenlerini tek noktadan uretir.
 *
 * <p>Yeni bir grafik eklenmek istendiginde sadece iki sey yapilmasi yeterlidir:</p>
 * <ol>
 *   <li>{@link GraphType} enum'una yeni bir sabit ekle.</li>
 *   <li>{@link #create} metodundaki switch'e yeni case ekle.</li>
 * </ol>
 */
public final class GraphFactory {

    /** Desteklenen grafik turleri. */
    public enum GraphType {
        /** VBO/GLCanvas tabanli radar ekrani. */
        RADAR,
        /** Selaleli (Waterfall) grafik. */
        WATERFALL,
        /** Dairesel (Circular) grafik. */
        CIRCULAR,
        /** Cizgi (Line) grafik. */
        LINE
    }

    private GraphFactory() { /* yardimci sinif */ }

    /**
     * Verilen ture gore bir grafik bileseni uretir.
     *
     * <p>Donus degeri hem bir {@link java.awt.Component} (Swing/AWT)
     * hem de bir {@link IGraph}'tir; ikisine de cast edilebilir.</p>
     *
     * @param type          Grafik turu; null olamaz.
     * @param config        Simulasyon konfigurasyonu; null olamaz.
     * @param entityManager Varlik yoneticisi; null olamaz.
     * @param frequency     Hedef FPS (yalnizca RADAR tipi icin gecerlidir).
     * @return Grafik bileseni (hem Component hem IGraph).
     * @throws IllegalArgumentException bilinmeyen tur icin.
     */
    public static Component create(GraphType type,
                                   SimulationConfig config,
                                   EntityManager entityManager,
                                   int frequency) {
        if (type == null || config == null || entityManager == null) {
            throw new IllegalArgumentException("GraphFactory parametreleri null olamaz.");
        }
        switch (type) {
            case RADAR:     return new RadarCanvas(frequency, entityManager);
            case WATERFALL: return new WaterfallGraph(config, entityManager);
            case CIRCULAR:  return new CircularGraph(config, entityManager);
            case LINE:      return new LineGraph(config, entityManager);
            default:        throw new IllegalArgumentException("Bilinmeyen grafik turu: " + type);
        }
    }
}
