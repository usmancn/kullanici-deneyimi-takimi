package com.radar.engine;

import com.radar.core.ISimulationEntity;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simülasyon sahnedeki tüm varlıkları ({@link ISimulationEntity}) yöneten sınıf.
 *
 * <p><b>Thread güvenliği:</b> Dahili liste olarak {@link CopyOnWriteArrayList}
 * kullanılır. Bu sayede:
 * <ul>
 *   <li>Simülasyon thread'i listeye yeni varlık ekleyebilir / ölü olanları kaldırabilir.</li>
 *   <li>JOGL render thread'i iterasyon sırasında {@code ConcurrentModificationException}
 *       almadan listeyi okuyabilir.</li>
 * </ul>
 * Yazma işlemleri nadir olduğundan (yalnızca spawn ve temizleme) bu yapının
 * getirdiği kopyalama maliyeti kabul edilebilir düzeydedir.</p>
 */
public final class EntityManager {

    /**
     * Aktif varlıkların thread-safe listesi.
     * Render thread'i bu listeyi doğrudan okur; simülasyon thread'i yazar.
     */
    private final CopyOnWriteArrayList<ISimulationEntity> entities;

    public EntityManager() {
        this.entities = new CopyOnWriteArrayList<>();
    }

    /**
     * Yeni bir varlığı sahneye ekler.
     *
     * @param entity Eklenecek varlık; null olamaz.
     * @throws IllegalArgumentException entity null ise.
     */
    public void addEntity(ISimulationEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Eklenecek varlik null olamaz.");
        }
        entities.add(entity);
    }

    /**
     * {@code isAlive()} değeri {@code false} olan tüm varlıkları listeden kaldırır.
     * Simülasyon thread'inde her tick sonunda çağrılmalıdır.
     */
    public void removeDeadEntities() {
        entities.removeIf(entity -> !entity.isAlive());
    }

    /**
     * Sahnedeki tüm aktif varlıkların değiştirilemez bir görünümünü döndürür.
     * Render thread'i bu listeyi iterasyon için kullanır.
     *
     * @return Değiştirilemez varlık listesi; boş olabilir, null olamaz.
     */
    public List<ISimulationEntity> getAll() {
        return Collections.unmodifiableList(entities);
    }

    /**
     * Sahnedeki aktif varlık sayısını döndürür.
     *
     * @return Varlık sayısı; negatif olamaz.
     */
    public int getEntityCount() {
        return entities.size();
    }

    /**
     * Tüm varlıkları sahneden kaldırır. Simülasyon sıfırlanırken kullanılır.
     */
    public void clearAll() {
        entities.clear();
    }
}
