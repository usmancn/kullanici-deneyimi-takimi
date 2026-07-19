package com.radar.sim.core;

import java.util.UUID;

/**
 * Simülasyon sahnesinde var olabilen her varlığın ana kontratı.
 *
 * Çizim, hareket ve güncelleme yeteneklerini tek çatı altında toplar.
 * Her varlığın eşsiz bir kimliği (UUID) ve ölü/diri durumu vardır.
 */
public interface ISimulationEntity extends IRenderable, IMovable, IUpdateable {

    /**
     * Varlığın evrensel eşsiz kimliğini döndürür.
     * Kimlik, oluşturulduğu anda bir kez atanır ve değişmez.
     *
     * @return UUID; null olamaz.
     */
    UUID getId();

    /**
     * Varlığın simülasyon sahnesinde hâlâ aktif olup olmadığını döndürür.
     * {@code false} döndüren varlıklar bir sonraki temizleme turunda
     * {@link com.radar.engine.EntityManager} tarafından listeden çıkarılır.
     *
     * @return {@code true} ise varlık aktif, {@code false} ise kaldırılmayı bekliyor.
     */
    boolean isAlive();
}
