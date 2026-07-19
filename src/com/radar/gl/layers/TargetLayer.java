package com.radar.gl.layers;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.radar.sim.core.ISimulationEntity;
import com.radar.gl.core.Camera;
import com.radar.gl.core.Geometry;
import com.radar.gl.core.ShaderProgram;
import com.radar.gl.core.TargetGeometry;
import com.radar.sim.model.Vector2D;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tarama cizgisinin gordugu hedefleri VBO ile yuksek hizda cizer.
 * Veriyi Osman'in EntityManager'indan gelen {@link ISimulationEntity} listesinden alir.
 * Gemilerin donma (freeze) ve mesafeye bagli sönüklesme (distance fade) mantigini yonetir.
 */
public class TargetLayer {

    /** Hedef simgesinin dunya birimindeki boyutu. */
    private static final float TARGET_SIZE = 10f; // 20f'den 10f'ye dusuruldu

    /** Gemi izinin (Blip) hafizada tutulacagi yapi */
    public static class Blip {
        public float x, y;
        public float hitScanY; // Cizgi uzerinden gectiginde cizginin konumu
    }

    private final TargetGeometry geometry;
    private final float[] matrix = new float[16];
    private final Map<UUID, Blip> memory = new HashMap<>();

    public TargetLayer(TargetGeometry geometry) { this.geometry = geometry; }

    public void draw(GL2 gl, ShaderProgram shader, Camera camera,
                     List<ISimulationEntity> detected, float currentScanY) {
                     
        // 1. Yeni tespit edilenleri hafizaya kaydet (Donma islemi)
        for (int i = 0; i < detected.size(); i++) {
            ISimulationEntity entity = detected.get(i);
            Blip blip = memory.computeIfAbsent(entity.getId(), k -> new Blip());
            Vector2D pos = entity.getPosition();
            blip.x = (float) pos.x;
            blip.y = (float) pos.y;
            blip.hitScanY = currentScanY;
        }

        shader.bindPosition(gl, geometry.position.id());
        shader.bindColor(gl, geometry.targetColor.id());

        float FADE_DISTANCE = Camera.WORLD_SIZE * 0.5f; // Ekranin yarisina gelince tamamen kaybolsun

        // 2. Hafizadakileri ciz ve eskiyenleri sil
        Iterator<Map.Entry<UUID, Blip>> it = memory.entrySet().iterator();
        while (it.hasNext()) {
            Blip blip = it.next().getValue();
            
            // Mesafeyi hesapla (wrap-around dikkate alinarak)
            float distance = currentScanY - blip.hitScanY;
            if (distance < 0) {
                distance += Camera.WORLD_SIZE;
            }

            if (distance > FADE_DISTANCE) {
                it.remove(); // Cok uzaklasti, hafizadan sil
                continue;
            }

            float opacity = 1.0f - (distance / FADE_DISTANCE);
            
            // Parlaklik sönükleşmesi
            shader.setTint(gl, 1.0f, 1.0f, 1.0f, opacity);

            camera.modelMatrix(matrix,
                    blip.x, blip.y,
                    TARGET_SIZE, TARGET_SIZE);
            shader.setMatrix(gl, matrix);
            gl.glDrawArrays(GL.GL_TRIANGLES, 0, Geometry.TARGET_VERTEX_COUNT);
        }
        shader.resetTint(gl);
    }
    
    // Minimap'in de bu hafizayi kullanabilmesi icin
    public Map<UUID, Blip> getMemory() {
        return memory;
    }
}
