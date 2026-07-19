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
        RadarLibrary.create()
                .withShipCount(50)
                .withFrequency(60)
                .withRadarSpeed(80.0)
                .withGraph(GraphType.RADAR)
                .start();
    }
}
