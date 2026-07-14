package com.radar.config;

/**
 * Simülasyonun tüm konfigürasyon parametrelerini tutan merkezi sınıf.
 * Hiçbir başka sınıfta sabit (magic number) kullanılmayacaktır;
 * tüm değerler buradan okunmalıdır.
 *
 * Değerler çalışma zamanında UI üzerinden güncellenebilir.
 */
public final class SimulationConfig {

    // -------------------------------------------------------------------------
    // Radar / Ekran Boyutları
    // -------------------------------------------------------------------------

    /** Radar matrisinin piksel cinsinden genişliği. */
    private int radarWidth = 1000;

    /** Radar matrisinin piksel cinsinden yüksekliği. */
    private int radarHeight = 1000;

    // -------------------------------------------------------------------------
    // Gemi Parametreleri
    // -------------------------------------------------------------------------

    /** Ekranda aynı anda bulunabilecek maksimum gemi sayısı. */
    private int maxShipCount = 20;

    /**
     * Her gemi (ve iz) için çizilecek piramit şeklinin boyutu (piksel).
     * Hem genişlik hem yükseklik bu değere eşittir (kare taban).
     */
    private int shipSize = 10;

    /**
     * Bir geminin bıraktığı izin kaç adım (snapshot) geriye gideceği.
     * Daha büyük değer → daha uzun kuyruk.
     */
    private int trailLength = 30;

    /**
     * Her iz adımının bir öncekine göre opaklık çarpanı.
     * 0.0 (tamamen şeffaf) ile 1.0 (hiç sönükleşme yok) arasında olmalı.
     * Örnek: 0.85f → her adım bir öncekinin %85 opaklığına sahip olur.
     */
    private float fadeFactor = 0.85f;

    // -------------------------------------------------------------------------
    // Simülasyon Motoru
    // -------------------------------------------------------------------------

    /**
     * Simülasyon motorunun saniyedeki güncelleme sayısı (Hz).
     * Bu değer yalnızca mantık güncellemelerini (update) etkiler;
     * JOGL render döngüsü bağımsız çalışır.
     */
    private int simulationHz = 60;

    /**
     * Gemilerin maksimum hareket hızı (piksel/saniye).
     * Spawn sırasında [minShipSpeed, maxShipSpeed] aralığında rastgele atanır.
     */
    private float maxShipSpeed = 120.0f;

    /** Gemilerin minimum hareket hızı (piksel/saniye). */
    private float minShipSpeed = 30.0f;

    // -------------------------------------------------------------------------
    // Metrik Paneli
    // -------------------------------------------------------------------------

    /**
     * CPU/GPU metrik panellerinin saniyedeki yenileme sayısı.
     * Yüksek değer daha akıcı grafik, daha fazla CPU yükü demektir.
     */
    private int metricsUpdateHz = 2;

    /**
     * Metrik grafiğinin tutacağı tarihsel veri noktası sayısı.
     * Grafik bu kadar "adım" geriye gider.
     */
    private int metricsHistorySize = 120;

    // -------------------------------------------------------------------------
    // Görsel / Renk
    // -------------------------------------------------------------------------

    /**
     * Gemi ve iz şekillerinin kırmızı kanalı (RGBA, 0.0–1.0).
     * Renk teması: kırmızı baskın.
     */
    private float shipColorR = 1.0f;

    /** Gemi ve iz şekillerinin yeşil kanalı. */
    private float shipColorG = 0.1f;

    /** Gemi ve iz şekillerinin mavi kanalı. */
    private float shipColorB = 0.1f;

    /** Radar arka plan kırmızı kanalı (koyu siyah). */
    private float bgColorR = 0.02f;

    /** Radar arka plan yeşil kanalı. */
    private float bgColorG = 0.02f;

    /** Radar arka plan mavi kanalı. */
    private float bgColorB = 0.02f;

    // -------------------------------------------------------------------------
    // Debug
    // -------------------------------------------------------------------------

    /**
     * Debug modu aktifse ekranda FPS, varlık sayısı ve koordinat bilgisi
     * Swing overlay olarak gösterilir. Üretim ortamında {@code false} olmalı.
     */
    private boolean debugMode = false;

    // -------------------------------------------------------------------------
    // Tekil örnek (Singleton)
    // -------------------------------------------------------------------------

    private static SimulationConfig instance;

    private SimulationConfig() {
        // Dışarıdan örneklenemez
    }

    /**
     * Singleton erişim noktası.
     * Thread-safe: çift kontrollü kilitleme (double-checked locking) kullanılır.
     */
    public static SimulationConfig getInstance() {
        if (instance == null) {
            synchronized (SimulationConfig.class) {
                if (instance == null) {
                    instance = new SimulationConfig();
                }
            }
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Getter / Setter
    // -------------------------------------------------------------------------

    public int getRadarWidth() {
        return radarWidth;
    }

    public void setRadarWidth(int radarWidth) {
        if (radarWidth <= 0) {
            throw new IllegalArgumentException("radarWidth pozitif olmalidir: " + radarWidth);
        }
        this.radarWidth = radarWidth;
    }

    public int getRadarHeight() {
        return radarHeight;
    }

    public void setRadarHeight(int radarHeight) {
        if (radarHeight <= 0) {
            throw new IllegalArgumentException("radarHeight pozitif olmalidir: " + radarHeight);
        }
        this.radarHeight = radarHeight;
    }

    public int getMaxShipCount() {
        return maxShipCount;
    }

    public void setMaxShipCount(int maxShipCount) {
        if (maxShipCount <= 0) {
            throw new IllegalArgumentException("maxShipCount pozitif olmalidir: " + maxShipCount);
        }
        this.maxShipCount = maxShipCount;
    }

    public int getShipSize() {
        return shipSize;
    }

    public void setShipSize(int shipSize) {
        if (shipSize <= 0) {
            throw new IllegalArgumentException("shipSize pozitif olmalidir: " + shipSize);
        }
        this.shipSize = shipSize;
    }

    public int getTrailLength() {
        return trailLength;
    }

    public void setTrailLength(int trailLength) {
        if (trailLength < 1) {
            throw new IllegalArgumentException("trailLength en az 1 olmalidir: " + trailLength);
        }
        this.trailLength = trailLength;
    }

    public float getFadeFactor() {
        return fadeFactor;
    }

    public void setFadeFactor(float fadeFactor) {
        if (fadeFactor < 0.0f || fadeFactor > 1.0f) {
            throw new IllegalArgumentException("fadeFactor [0.0, 1.0] araliginda olmalidir: " + fadeFactor);
        }
        this.fadeFactor = fadeFactor;
    }

    public int getSimulationHz() {
        return simulationHz;
    }

    public void setSimulationHz(int simulationHz) {
        if (simulationHz <= 0) {
            throw new IllegalArgumentException("simulationHz pozitif olmalidir: " + simulationHz);
        }
        this.simulationHz = simulationHz;
    }

    public float getMaxShipSpeed() {
        return maxShipSpeed;
    }

    public void setMaxShipSpeed(float maxShipSpeed) {
        this.maxShipSpeed = maxShipSpeed;
    }

    public float getMinShipSpeed() {
        return minShipSpeed;
    }

    public void setMinShipSpeed(float minShipSpeed) {
        this.minShipSpeed = minShipSpeed;
    }

    public int getMetricsUpdateHz() {
        return metricsUpdateHz;
    }

    public void setMetricsUpdateHz(int metricsUpdateHz) {
        if (metricsUpdateHz <= 0) {
            throw new IllegalArgumentException("metricsUpdateHz pozitif olmalidir: " + metricsUpdateHz);
        }
        this.metricsUpdateHz = metricsUpdateHz;
    }

    public int getMetricsHistorySize() {
        return metricsHistorySize;
    }

    public void setMetricsHistorySize(int metricsHistorySize) {
        if (metricsHistorySize <= 0) {
            throw new IllegalArgumentException("metricsHistorySize pozitif olmalidir: " + metricsHistorySize);
        }
        this.metricsHistorySize = metricsHistorySize;
    }

    public float getShipColorR() {
        return shipColorR;
    }

    public void setShipColorR(float shipColorR) {
        this.shipColorR = shipColorR;
    }

    public float getShipColorG() {
        return shipColorG;
    }

    public void setShipColorG(float shipColorG) {
        this.shipColorG = shipColorG;
    }

    public float getShipColorB() {
        return shipColorB;
    }

    public void setShipColorB(float shipColorB) {
        this.shipColorB = shipColorB;
    }

    public float getBgColorR() {
        return bgColorR;
    }

    public float getBgColorG() {
        return bgColorG;
    }

    public float getBgColorB() {
        return bgColorB;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
}
