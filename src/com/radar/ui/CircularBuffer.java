package com.radar.ui;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Sabit kapasiteli dairesel arabellek (ring buffer).
 *
 * <p>Kapasite dolduğunda en eski eleman otomatik olarak kaldırılır.
 * Metrik geçmişini tutmak için kullanılır.</p>
 *
 * <p><b>Thread güvenliği:</b> Bu sınıf tek thread kullanımı için
 * tasarlanmıştır. Swing EDT üzerinden erişilmelidir.</p>
 */
public final class CircularBuffer {

    private final Deque<Double> buffer;
    private final int capacity;

    /**
     * Belirtilen kapasite ile yeni bir dairesel arabellek oluşturur.
     *
     * @param capacity Tutulacak maksimum eleman sayısı; pozitif olmalıdır.
     */
    public CircularBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Kapasite pozitif olmalidir: " + capacity);
        }
        this.capacity = capacity;
        this.buffer   = new ArrayDeque<>(capacity);
    }

    /**
     * Arabelleke yeni bir değer ekler.
     * Kapasite doluysa en eski eleman kaldırılır.
     *
     * @param value Eklenecek değer.
     */
    public void add(double value) {
        if (buffer.size() >= capacity) {
            buffer.removeFirst();
        }
        buffer.addLast(value);
    }

    /**
     * Arabellekteki tüm değerlerin anlık görüntüsünü dizi olarak döndürür.
     * En eski değer indeks 0'da, en yeni son indekste bulunur.
     *
     * @return Değerler dizisi; boş olabilir, null olamaz.
     */
    public double[] toArray() {
        Double[] boxed  = buffer.toArray(new Double[0]);
        double[] result = new double[boxed.length];
        for (int i = 0; i < boxed.length; i++) {
            result[i] = boxed[i];
        }
        return result;
    }

    /**
     * Arabellekteki mevcut eleman sayısını döndürür.
     */
    public int size() {
        return buffer.size();
    }

    /**
     * Arabellekteki maksimum kapasite.
     */
    public int getCapacity() {
        return capacity;
    }
}
