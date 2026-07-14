package com.radar.core;

/**
 * Simülasyon döngüsüne dahil olup her tick'te güncellenmesi gereken
 * her varlığın uygulaması gereken kontrat.
 *
 * <p>Bu metot yalnızca simülasyon thread'inden çağrılır.</p>
 */
public interface IUpdateable {

    /**
     * Nesnenin iç durumunu (iz, ömür, animasyon vb.) günceller.
     *
     * @param deltaTime Son güncellemeden bu yana geçen süre (saniye cinsinden).
     */
    void update(double deltaTime);
}
