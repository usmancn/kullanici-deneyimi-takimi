package com.radar.gl.layers;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.radar.sim.core.ISimulationEntity;
import com.radar.gl.core.Camera;
import com.radar.gl.core.CircularProjection;
import com.radar.gl.core.GainColor;
import com.radar.gl.core.Geometry;
import com.radar.gl.core.ShaderProgram;
import com.radar.gl.core.TargetGeometry;
import com.radar.sim.model.Ship;
import com.radar.sim.model.Vector2D;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sonar tabanli dairesel radarda hedefleri cizer.
 * Hedefin merkezden uzakligi ile buyuyen dalganin yari capi arasindaki
 * mesafeye gore (radial distance) hedefleri sonuklestirir.
 */
public class CircularTargetLayer {

    private static final float TARGET_SIZE = 10f;

    public static class CircularBlip {
        public float x, y;
        public float hitRadius; // Vuruldugu anki yaricapi
        /** Geminin gain factor'u; renklendirme ve filtre icin saklanir. Varsayilan 1.0. */
        public double gain = 1.0;
        /** Geminin dunya birimindeki govde boyutlari (metre). Varsayilan TARGET_SIZE. */
        public float width  = TARGET_SIZE;
        public float height = TARGET_SIZE;
        public java.util.List<float[]> trail = new java.util.ArrayList<>();
    }

    private final TargetGeometry geometry;
    private final float[] matrix = new float[16];
    private final Map<UUID, CircularBlip> memory = new HashMap<>();

    public CircularTargetLayer(TargetGeometry geometry) { this.geometry = geometry; }

    public void draw(GL2 gl, ShaderProgram shader, Camera camera,
                     List<ISimulationEntity> detected, float currentScanRadius) {
                     
        float cx = Camera.WORLD_SIZE / 2f;
        float cy = Camera.WORLD_SIZE / 2f;
        float maxRadius = Camera.WORLD_SIZE / 2f * 0.92f;

        // Vurulanlari guncelle
        for (int i = 0; i < detected.size(); i++) {
            ISimulationEntity entity = detected.get(i);
            CircularBlip blip = memory.computeIfAbsent(entity.getId(), k -> new CircularBlip());
            Vector2D pos = entity.getPosition();
            // Polar esleme: x -> aci, y -> menzil. Gemi diskteki gorsel konumuna tasinir.
            float dispX = (float) CircularProjection.worldX(pos.x, pos.y);
            float dispY = (float) CircularProjection.worldY(pos.x, pos.y);
            if (entity instanceof Ship) {
                Ship ship = (Ship) entity;
                blip.gain   = ship.getGainFactor();
                blip.width  = (float) ship.getWidth();
                blip.height = (float) ship.getHeight();
            }

            if (blip.trail.isEmpty()) {
                blip.x = dispX;
                blip.y = dispY;
                blip.trail.add(new float[]{blip.x, blip.y});
            } else {
                if (Math.abs(blip.x - dispX) > 0.5f || Math.abs(blip.y - dispY) > 0.5f) {
                    blip.trail.add(new float[]{blip.x, blip.y});
                    if (blip.trail.size() > 15) {
                        blip.trail.remove(0);
                    }
                }
                blip.x = dispX;
                blip.y = dispY;
            }

            blip.hitRadius = (float) CircularProjection.radius(pos.y);
        }

        shader.bindPosition(gl, geometry.position.id());
        // Kirmizi geceli taban gradyani; uzerine gain'e bagli mavi eklenecek.
        shader.bindColor(gl, geometry.targetColor.id());

        Iterator<Map.Entry<UUID, CircularBlip>> it = memory.entrySet().iterator();
        while (it.hasNext()) {
            CircularBlip blip = it.next().getValue();

            // Gain filtresi: aralik disindaki gemileri gizle.
            if (!GainColor.passesFilter(blip.gain)) {
                continue;
            }

            // Dalganin hedeften ne kadar uzaklastigini hesapla (wrap-around)
            float radialDistance = currentScanRadius - blip.hitRadius;
            if (radialDistance < 0) {
                radialDistance += maxRadius; // Dalga tekrar bastan basladiysa
            }

            // Dalgadan uzaklastikca kararacak (%80 oraninda kararabilir max)
            float opacity = 1.0f - 0.8f * (radialDistance / maxRadius);
            if (opacity < 0.2f) opacity = 0.2f;

            // Gercekci Radar Beamwidth (Huzme) Etkisi:
            // Radyal kalinlik (Pulse length) sabittir. Tegetsel genislik (Beam width) uzaklikla artar.
            double dx = blip.x - cx;
            double dy = blip.y - cy;
            double distSq = dx*dx + dy*dy;
            float distFromCenter = (float) Math.sqrt(distSq);
            
            // 2.5 derecelik bir radar beam width varsayiyoruz.
            // Minimum tegetsel genislik gemi genisligine, radyal kalinlik gemi yuksekligine baglidir.
            // Konumlar menzil ekseninde 0.46 ile sikistirildigi icin gemi boyutu da ayni
            // oranla (rangeScale) olceklenir; boylece gemiler 0..1000 olceginde buyuk gorunmez.
            float sizeScale = CircularProjection.rangeScale();
            float beamAngle = (float) Math.toRadians(2.5);
            float arcWidth = Math.max(blip.width * sizeScale, distFromCenter * beamAngle);
            float thickness = blip.height * sizeScale; // Radyal kalinlik gemi boyutuna gore

            float theta = (float) Math.atan2(dy, dx);
            float cos = (float) Math.cos(theta);
            float sin = (float) Math.sin(theta);
            
            float hw = arcWidth / 2f;
            float ht = thickness / 2f;
            
            float sx = 2f / camera.rangeX();
            float sy = 2f / camera.rangeY();

            for (int j = 0; j < blip.trail.size(); j++) {
                float[] oldPos = blip.trail.get(j);
                float ageFactor = (float)(j + 1) / (blip.trail.size() + 1);
                float pointOpacity = opacity * ageFactor * 0.6f;
                if (pointOpacity < 0.05f) pointOpacity = 0.05f;

                GainColor.applyGainColor(gl, shader, blip.gain, pointOpacity);

                // Gecmis izleri biraz daha kucuk ciziyoruz (yay formunu koruyarak)
                float hwTrail = hw * 0.7f;
                float htTrail = ht * 0.7f;
                
                float dxTrail = oldPos[0] - cx;
                float dyTrail = oldPos[1] - cy;
                float thetaTrail = (float) Math.atan2(dyTrail, dxTrail);
                float cosT = (float) Math.cos(thetaTrail);
                float sinT = (float) Math.sin(thetaTrail);

                matrix[0] = hwTrail * (-sinT) * sx; matrix[4] = htTrail * cosT * sx; matrix[8] = 0; matrix[12] = (oldPos[0] - camera.minX()) * sx - 1f;
                matrix[1] = hwTrail * cosT * sy;    matrix[5] = htTrail * sinT * sy; matrix[9] = 0; matrix[13] = (oldPos[1] - camera.minY()) * sy - 1f;
                matrix[2] = 0;                      matrix[6] = 0;                   matrix[10]= 1; matrix[14] = 0;
                matrix[3] = 0;                      matrix[7] = 0;                   matrix[11]= 0; matrix[15] = 1;

                shader.setMatrix(gl, matrix);
                gl.glDrawArrays(GL.GL_TRIANGLES, 0, Geometry.TARGET_VERTEX_COUNT);
            }

            GainColor.applyGainColor(gl, shader, blip.gain, opacity);
            matrix[0] = hw * (-sin) * sx; matrix[4] = ht * cos * sx; matrix[8] = 0; matrix[12] = (blip.x - camera.minX()) * sx - 1f;
            matrix[1] = hw * cos * sy;    matrix[5] = ht * sin * sy; matrix[9] = 0; matrix[13] = (blip.y - camera.minY()) * sy - 1f;
            matrix[2] = 0;                matrix[6] = 0;             matrix[10]= 1; matrix[14] = 0;
            matrix[3] = 0;                matrix[7] = 0;             matrix[11]= 0; matrix[15] = 1;
            
            shader.setMatrix(gl, matrix);
            gl.glDrawArrays(GL.GL_TRIANGLES, 0, Geometry.TARGET_VERTEX_COUNT);
        }

        GainColor.reset(gl, shader);
    }

    public Map<UUID, CircularBlip> getMemory() {
        return memory;
    }
}
