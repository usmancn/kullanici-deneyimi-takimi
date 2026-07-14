package com.radar.core;

import com.jogamp.opengl.GL2;

/**
 * JOGL bağlamında kendini çizebilen her nesnenin uygulaması gereken kontrat.
 * Implementasyon sınıfları, GL2 nesnesi üzerinden OpenGL çağrılarını
 * doğrudan gerçekleştirir.
 *
 * <p><b>Thread güvenliği:</b> Bu metot yalnızca JOGL render thread'inden
 * (GLEventListener#display içinden) çağrılmalıdır. EDT üzerinden çağrılmamalıdır.</p>
 */
public interface IRenderable {

    /**
     * Nesneyi mevcut OpenGL bağlamına çizer.
     *
     * @param gl Aktif GL2 bağlamı; null olmamalıdır.
     */
    void render(GL2 gl);
}
