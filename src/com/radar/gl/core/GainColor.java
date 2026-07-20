package com.radar.gl.core;

import com.jogamp.opengl.GL2;
import com.radar.config.SimulationConfig;

/**
 * Gain factor'e bagli gorsel yardimcilar.
 *
 * <p><b>Renklendirme:</b> hedefin mevcut (kirmizi geceli) taban gradyani korunur;
 * uzerine gain ile orantili bir <i>mavi</i> ton EKLENIR (toplamsal). Shader'da:
 * <pre>
 *   outColor = inColor * tint + addColor
 *   tint     = (1, 1, 1, opacity)              // kirmizi taban aynen kalir
 *   addColor = (0, 0, gain * BLUE_STRENGTH, 0)  // gain kadar ekstra mavi
 * </pre>
 * Boylece dusuk gain'li gemi neredeyse saf kirmizi kalir, gain arttikca gemi
 * gitgide mor/mavimsi bir tona kayar. Eklenen mavi de blend sirasinda opaklikla
 * carpildigi icin mesafe sonumlemesiyle tutarli sekilde solar.
 *
 * <p><b>Filtre:</b> {@link SimulationConfig} icindeki gainFilterMin/Max araligina
 * gore bir gemi cizilir ya da gizlenir.
 */
public final class GainColor {

    private GainColor() { /* yardimci sinif */ }

    /**
     * gain = 1 iken eklenecek mavi miktari. 1.0'a yaklastikca yuksek gain'li
     * gemiler daha mavi/mor olur; kucultmek kirmiziyi daha baskin birakir.
     */
    private static final float BLUE_STRENGTH = 0.85f;

    /**
     * Verilen gain ve opaklik icin renk uniform'larini ayarlar.
     * Taban gradyan (kirmizi) korunur, uzerine gain kadar mavi eklenir.
     *
     * @param gain    0..1 arasi gain factor.
     * @param opacity Mesafeye bagli sonukleme opakligi.
     */
    public static void applyGainColor(GL2 gl, ShaderProgram shader, double gain, float opacity) {
        float g = clamp01((float) gain);
        shader.setTint(gl, 1f, 1f, 1f, opacity);
        shader.setAddColor(gl, 0f, 0f, g * BLUE_STRENGTH, 0f);
    }

    /** Renk uniform'larini notr degerlere dondurur (diger katmanlar etkilenmesin diye). */
    public static void reset(GL2 gl, ShaderProgram shader) {
        shader.resetTint(gl);
        shader.resetAddColor(gl);
    }

    /**
     * Geminin aktif gain filtresi araliginda olup olmadigini dondurur.
     * Filtre disindaki gemiler cizilmemelidir.
     */
    public static boolean passesFilter(double gain) {
        SimulationConfig config = SimulationConfig.getInstance();
        return gain >= config.getGainFilterMin() && gain <= config.getGainFilterMax();
    }

    private static float clamp01(float value) {
        if (value < 0.0f) return 0.0f;
        if (value > 1.0f) return 1.0f;
        return value;
    }
}
