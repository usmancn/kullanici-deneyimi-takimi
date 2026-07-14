package com.radar.core;

import java.util.UUID;

/**
 * Simülasyon sahnesinde var olabilen her varlığın ana kontratı.
 *
 * <p>Bu interface, çizim ({@link IRenderable}), hareket ({@link IMovable})
 * ve güncelleme ({@link IUpdateable}) yeteneklerini tek çatı altında toplar.
 * Yeni bir varlık türü (ör. mayın, boya noktası) eklenecekse yalnızca
 * bu interface'i implement etmesi yeterlidir; motorun geri kalanı
 * değişmeden çalışmaya devam eder.</p>
 *
 * <p>Her varlığın eşsiz bir kimliği ({@link UUID}) ve ölü/diri durumu vardır.
 * Motor, {@code isAlive()} döndüren varlıkları sahneden kaldırır.</p>
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
