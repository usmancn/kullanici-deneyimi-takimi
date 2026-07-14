package com.radar.core;

import com.jogamp.opengl.GL2;
import com.radar.renderer.RenderContext;

/**
 * JOGL bağlamında kendini çizebilen her nesnenin uygulaması gereken kontrat.
 *
 * <p>Implementasyon sınıfları, GL2 nesnesi ve {@link RenderContext} üzerinden
 * sweep pozisyonuna erişerek sweep-tabanlı opaklık hesaplaması yapabilir.</p>
 *
 * <p><b>Thread güvenliği:</b> Bu metot yalnızca JOGL render thread'inden
 * (GLEventListener#display içinden) çağrılmalıdır. EDT üzerinden çağrılmamalıdır.</p>
 */
public interface IRenderable {

    /**
     * Nesneyi mevcut OpenGL bağlamına çizer.
     *
     * @param gl  Aktif GL2 bağlamı; null olmamalıdır.
     * @param ctx Render bağlamı (sweep pozisyonu vb.); null olmamalıdır.
     */
    void render(GL2 gl, RenderContext ctx);
}
