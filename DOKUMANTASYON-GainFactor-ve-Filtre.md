# Gain Factor, Mavi Renklendirme ve Gain Filtresi — Değişiklik Dokümantasyonu

Tarih: 2026-07-20

Bu doküman, gemilerin **rastgele boyutlarla** üretilmesi, boyuta (alana) bağlı bir
**gain factor** kazanması, bu gain factor'e göre **maviye boyanması** ve radara bir
**gain filtresi** (çift tutamaklı RangeSlider) eklenmesi için yapılan tüm değişiklikleri
özetler.

---

## 1. Özet / İstenenler

1. Gemiler artık hep aynı boyutta değil; **x ve y boyutları 5–20 m arasında** bağımsız
   rastgele üretiliyor.
2. Her gemi, **alanıyla doğru orantılı** bir gain factor alıyor:
   - `20 × 20` (en büyük) → gain **1.00**
   - `5 × 5` (en küçük) → gain **0.20**
   - Aradaki gemiler doğrusal olarak `0.20 – 1.00` aralığına eşleniyor.
3. Gemiler görselleştirilirken **gain factor kadar mavi** oluyor; böylece renkle
   birbirinden ayırt edilebiliyorlar.
4. Radara **gain filtresi** eklendi: çift tutamaklı bir slider ile bir `[alt, üst]`
   gain aralığı seçilip aralık dışındaki gemiler gizlenebiliyor. Slider yapısı örnek
   `RangeSlider.java`'dan uyarlandı, koyu/mavi temaya göre renklendirildi ve projeye
   entegre edildi.

---

## 2. Gain Factor Formülü

Alan doğrusal olarak gain aralığına taşınır (alanla doğru orantılı, artan):

```
minAlan = minDim²            (5²  = 25)
maxAlan = maxDim²            (20² = 400)
alan    = width × height

normalized = (alan − minAlan) / (maxAlan − minAlan)   // [0, 1] arasına kırpılır
gain       = minGain + normalized × (maxGain − minGain)
```

Varsayılan değerlerle (`minGain = 0.20`, `maxGain = 1.00`):

| Gemi          | Alan | gain  |
|---------------|------|-------|
| 5 × 5         | 25   | 0.20  |
| 20 × 20       | 400  | 1.00  |
| 10 × 10       | 100  | 0.36  |
| 5 × 20        | 100  | 0.36  |

> Not: "Doğru orantı", istenen iki uç nokta (25→0.20 ve 400→1.00) sağlanacak şekilde,
> alan ile artan **doğrusal eşleme** olarak yorumlandı. Saf `gain = k·alan` biçimi bu iki
> ucu aynı anda tutturamadığı için (25 ve 400 için farklı k gerekir) doğrusal interpolasyon
> tercih edildi.

---

## 3. Boyuta Göre Çizim

Gemiler önceden sabit bir `TARGET_SIZE` (10 dünya birimi) ile çiziliyordu; artık her gemi
kendi **width × height** boyutunda çiziliyor. Boyutlar spawn'da atanan metre değerleridir
ve doğrudan dünya birimi olarak kullanılır (dünya 1000×1000, gemiler ~5–20 birim).

- **Kare radar:** hedef karesi `blip.width × blip.height` olarak ölçeklenir (izler %70).
- **Yuvarlak radar:** radyal kalınlık (pulse length) gemi **yüksekliğine**, minimum teğetsel
  genişlik (beam width) gemi **genişliğine** bağlanır; uzaklıkla huzme yine açılır.
- **Minimap'ler:** blip'ler de gerçek boyutlarına göre çizilir.

Boyut aktarımı `Blip` / `CircularBlip` yapılarına eklenen `width` ve `height` alanlarıyla
yapılır; gemi tespit edildiğinde `Ship.getWidth()/getHeight()` değerleri kopyalanır.

## 4. Renklendirme Mantığı (kırmızı taban + gain'e göre ekstra mavi)

Mevcut **kırmızı geçişli** taban gradyanı (`targetColors()`) korunur; üzerine gain ile
orantılı bir **mavi ton eklenir**. Çarpımsal tint ile mavi *eklenemeyeceği* için shader'a
toplamsal bir renk terimi (`addColor`) eklendi:

```
outColor = inColor * tint + addColor
tint     = (1, 1, 1, opacity)                 // kırmızı taban aynen kalır
addColor = (0, 0, gain * BLUE_STRENGTH, 0)    // gain kadar ekstra mavi
```

`BLUE_STRENGTH = 0.85` (GainColor içinde sabit). Sonuç:

- **Düşük gain (0.20):** neredeyse saf kırmızı, çok hafif mavi.
- **Yüksek gain (1.00):** kırmızı çekirdek + belirgin mavi → mor/mavimsi ton.

Eklenen mavi de blend sırasında opaklıkla çarpıldığı için mesafe sönümlemesiyle tutarlı
solar. Mavinin dozunu değiştirmek için tek nokta: `GainColor.BLUE_STRENGTH`.

---

## 5. Gain Filtresi

- UI'da çift tutamaklı `GainRangeSlider` ile `[alt, üst]` gain aralığı seçilir
  (slider 0–100, gain 0.00–1.00 ile eşlenir).
- Seçilen değerler `SimulationConfig.gainFilterMin / gainFilterMax` alanlarına yazılır
  (bu alanlar `volatile`; EDT yazar, render thread okur).
- Her karede render thread, `GainColor.passesFilter(gain)` ile aralık dışındaki
  gemileri **hem ana radarda hem minimap'te** çizmeden atlar.
- Filtre paneli hem **kare radar** hem **yuvarlak (circular) radar** sekmelerine eklendi.
  İki panel de aynı config'i paylaşır ve birbirini senkron tutar (statik kayıt listesi).

---

## 6. Değiştirilen / Eklenen Dosyalar

### Eklenen
| Dosya | Görevi |
|-------|--------|
| `gl/core/GainColor.java` | Kırmızı taban + gain'e bağlı ekstra mavi uygulama + filtre kontrolü (tek merkez) |
| `ui/GainRangeSlider.java` | Çift tutamaklı slider (örnek RangeSlider'dan, mavi tema) |
| `ui/GainFilterPanel.java` | Slider + etiket; config'e yazar, kardeş panelleri senkronlar |

### Değiştirilen
| Dosya | Değişiklik |
|-------|-----------|
| `config/SimulationConfig.java` | `minShipDimension`, `maxShipDimension`, `minGainFactor`, `maxGainFactor`, `gainFilterMin`, `gainFilterMax` alanları + getter/setter'lar |
| `sim/model/Ship.java` | `width`, `height`, `gainFactor` alanları; rastgele boyut ve alan→gain hesabı; getter'lar |
| `gl/core/ShaderProgram.java` | Toplamsal `addColor` uniform'u: `outColor = inColor * tint + addColor` (+ set/reset) |
| `gl/layers/TargetLayer.java` | `Blip.gain/width/height`; boyuta göre çizim; gain'e bağlı mavi + filtre |
| `gl/layers/CircularTargetLayer.java` | `CircularBlip.gain/width/height`; boyuta göre huzme; aynı renk + filtre |
| `gl/ui/Minimap.java` | Blip'leri boyut + gain rengiyle çizme + filtre |
| `gl/ui/CircularMinimap.java` | Aynı (yuvarlak minimap) |
| `ui/MainFrame.java` | Radar ve Circular sekmelerini `GainFilterPanel` ile (BorderLayout, SOUTH) sarmalama |

---

## 7. Ayarlanabilir Parametreler (SimulationConfig)

Tüm değerler koddaki sabitlerden değil, `SimulationConfig` üzerinden okunur:

| Parametre | Varsayılan | Açıklama |
|-----------|-----------|----------|
| `minShipDimension` | 5.0 | Gemi kenarının alt sınırı (m) |
| `maxShipDimension` | 20.0 | Gemi kenarının üst sınırı (m) |
| `minGainFactor` | 0.20 | En küçük gemiye atanacak gain |
| `maxGainFactor` | 1.00 | En büyük gemiye atanacak gain |
| `gainFilterMin` | 0.0 | Filtre alt eşiği (başlangıçta tüm gemiler görünür) |
| `gainFilterMax` | 1.0 | Filtre üst eşiği |

---

## 8. Notlar / Bilinçli Kararlar

- **Filtre çizim aşamasında** uygulanıyor (tespit/hafıza aşamasında değil). Böylece
  slider anlık oynatıldığında gemiler yeniden tespit edilmeyi beklemeden görünüp
  kayboluyor; filtre gevşetildiğinde hafızadaki blip'ler tekrar beliriyor.
- Gemiler şu an **hareketsiz** (`Ship.move()` bilinçli boş). Gain factor ve boyut spawn
  anında bir kez atanıp sabit kalıyor; hareket geri açılsa bile değişmez.
- Boyutlar dünya birimi olarak (metre ≈ piksel) doğrudan çizime aktarıldı. Gemileri daha
  büyük/küçük göstermek isterseniz `Ship.getWidth()/getHeight()` değerlerine ortak bir
  ölçek çarpanı uygulamak yeterli (örn. layer'da `blip.width * SCALE`).
- `addColor` yalnızca hedef/minimap çizimlerinde ayarlanıp her seferinde `GainColor.reset`
  ile sıfırlanır; grid, tarama çizgisi, işaret gibi diğer katmanlar etkilenmez.
- Derleme JOGL classpath'i ile hatasız geçti (Java 21 `javac`).
