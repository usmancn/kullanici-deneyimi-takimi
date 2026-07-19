package com.radar.ui;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Sabit kapasiteli dairesel tampon (halka arabelleği).
 * Dolu olduğunda en eski öğeyi otomatik olarak siler.
 * MetricsPanel gibi zaman serisi grafiklerinde kullanılır.
 */
public final class CircularBuffer {

    private final Deque<Double> buffer;
    private final int capacity;

    /**
     * @param capacity Maksimum eleman sayısı; en az 1 olmalıdır.
     */
    public CircularBuffer(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Kapasite en az 1 olmalidir.");
        this.capacity = capacity;
        this.buffer   = new ArrayDeque<>(capacity);
    }

    /**
     * Tampona yeni bir değer ekler.
     * Doluysa en eski değer otomatik olarak çıkarılır.
     */
    public synchronized void add(double value) {
        if (buffer.size() >= capacity) buffer.pollFirst();
        buffer.addLast(value);
    }

    /**
     * Tampondaki değerlerin anlık kopyasını döndürür (en eskiden en yeniye).
     */
    public synchronized double[] snapshot() {
        double[] result = new double[buffer.size()];
        int i = 0;
        for (double v : buffer) result[i++] = v;
        return result;
    }

    /** Tampondaki mevcut eleman sayısı. */
    public synchronized int size() {
        return buffer.size();
    }

    /** Maksimum kapasite. */
    public int getCapacity() {
        return capacity;
    }
}
