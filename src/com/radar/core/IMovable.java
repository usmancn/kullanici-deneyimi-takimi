package com.radar.core;

import com.radar.model.Vector2D;

/**
 * Sahada hareket edebilen her varlığın uygulaması gereken kontrat.
 * Pozisyon ve hız bilgisini soyutlar; hareket kararları implementasyona bırakılır.
 *
 * <p>Hareket hesaplamaları simülasyon thread'inde yapılır ve delta-time
 * (geçen süre saniye cinsinden) parametresi ile kare hızından bağımsız
 * tutulur.</p>
 */
public interface IMovable {

    /**
     * Nesneyi verilen delta-time kadar ilerletir.
     * Pozisyon güncellemesi, sınır kontrolü ve yön değişikliği bu metotta yapılır.
     *
     * @param deltaTime Son güncellemeden bu yana geçen süre (saniye cinsinden).
     */
    void move(double deltaTime);

    /**
     * Nesnenin mevcut dünya koordinatlarındaki pozisyonunu döndürür.
     *
     * @return Mevcut pozisyon; null olamaz.
     */
    Vector2D getPosition();

    /**
     * Nesnenin pozisyonunu doğrudan atar (ör. spawn sırasında).
     *
     * @param position Yeni pozisyon; null olamaz.
     */
    void setPosition(Vector2D position);

    /**
     * Nesnenin mevcut hız vektörünü döndürür (piksel/saniye).
     *
     * @return Hız vektörü; null olamaz.
     */
    Vector2D getVelocity();
}
