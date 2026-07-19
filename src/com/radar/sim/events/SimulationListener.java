package com.radar.sim.events;

import com.radar.sim.core.ISimulationEntity;

import java.util.List;

/**
 * Simülasyon durumu degistiginde dinleyicilere bildirim gonder.
 *
 * <p>Lambda ile kullanilabilir (Functional Interface):</p>
 * <pre>{@code
 * engine.addListener(entities -> myGraph.update(entities));
 * }</pre>
 */
@FunctionalInterface
public interface SimulationListener {

    /**
     * Simülasyon her güncellendiğinde çağrılır.
     *
     * @param entities Tüm aktif varlikların anlık kopyası; null olmaz.
     */
    void onUpdate(List<ISimulationEntity> entities);
}
