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
        public float hitScanY;
        public java.util.List<float[]> trail = new java.util.ArrayList<>();
    }

    private final TargetGeometry geometry;
    private final float[] matrix = new float[16];
    private final Map<UUID, Blip> memory = new HashMap<>();

    public TargetLayer(TargetGeometry geometry) { this.geometry = geometry; }

    public void draw(GL2 gl, ShaderProgram shader, Camera camera,
                     List<ISimulationEntity> detected, float currentScanY) {
                     
        // 1. Yeni tespit edilenleri hafizaya kaydet (Donma islemi ve Iz olusturma)
        for (int i = 0; i < detected.size(); i++) {
            ISimulationEntity entity = detected.get(i);
            Blip blip = memory.computeIfAbsent(entity.getId(), k -> new Blip());
            Vector2D pos = entity.getPosition();
            
            if (blip.trail.isEmpty()) {
                blip.x = (float) pos.x;
                blip.y = (float) pos.y;
                blip.trail.add(new float[]{blip.x, blip.y});
            } else {
                // Eger gemi hareket etmisse ize ekle. 
                // Sabit duran gemiler icin ayni noktayi 15 kere ust uste eklemek 
                // opacity (saydamlik) birikmesine yol acar ve geminin sonuklesmesini engeller.
                if (Math.abs(blip.x - pos.x) > 0.5f || Math.abs(blip.y - pos.y) > 0.5f) {
                    blip.trail.add(new float[]{blip.x, blip.y});
                    if (blip.trail.size() > 15) {
                        blip.trail.remove(0);
                    }
                }
                blip.x = (float) pos.x;
                blip.y = (float) pos.y;
            }
            blip.hitScanY = currentScanY;
        }

        shader.bindPosition(gl, geometry.position.id());
        shader.bindColor(gl, geometry.targetColor.id());

        // 2. Hafizadakileri ciz (Eski hedefler tamamen silinmez, sadece 0.2 opacity'ye kadar kararir)
        Iterator<Map.Entry<UUID, Blip>> it = memory.entrySet().iterator();
        while (it.hasNext()) {
            Blip blip = it.next().getValue();
            
            // Mesafeyi hesapla (wrap-around dikkate alinarak)
            float distance = currentScanY - blip.hitScanY;
            if (distance < 0) {
                distance += Camera.WORLD_SIZE;
            }

            // En fazla %80 oraninda kararacak (1.0 -> 0.2)
            float opacity = 1.0f - 0.8f * (distance / Camera.WORLD_SIZE);
            if (opacity < 0.2f) opacity = 0.2f;

            // Izleri ciz
            for (int j = 0; j < blip.trail.size(); j++) {
                float[] oldPos = blip.trail.get(j);
                float ageFactor = (float)(j + 1) / (blip.trail.size() + 1); // 0.0 ile 1.0 arasi, eski noktalar daha silik
                float pointOpacity = opacity * ageFactor * 0.6f;
                if (pointOpacity < 0.05f) pointOpacity = 0.05f;
                
                shader.setTint(gl, 1.0f, 1.0f, 1.0f, pointOpacity);
                camera.modelMatrix(matrix, oldPos[0], oldPos[1], TARGET_SIZE * 0.7f, TARGET_SIZE * 0.7f);
                shader.setMatrix(gl, matrix);
                gl.glDrawArrays(GL.GL_TRIANGLES, 0, Geometry.TARGET_VERTEX_COUNT);
            }

            // Guncel konumu ciz
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
