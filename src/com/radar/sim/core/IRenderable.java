package com.radar.sim.core;

/**
 * Sahnede çizilebilir bir varlık.
 *
 * <p>Render detayları (OpenGL çağrıları) artık Fatih'in VBO katmanlarında
 * ({@link com.radar.gl.layers}) yapıldığından bu arayüz bilinçli olarak
 * sade tutulmuştur. Gelecekte grafik katmanına özel callback ihtiyacı
 * doğarsa buraya eklenebilir.</p>
 */
public interface IRenderable {
    // Şimdilik boş — işaretleyici (marker) arayüz olarak tutulur.
    // Grafik katmanı ISimulationEntity'yi doğrudan sorgular.
}
