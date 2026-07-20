# Kare Radar vs Circular Radar — Menzil Etiketi Farkı (1000 vs 460)

Tarih: 2026-07-20

## Soru

İki radar da aynı **1000 × 1000** simülasyon dünyasını kullanıyor. Buna rağmen:

- **Kare radar**ın en dış eksen etiketi ~**1000** (dünya kenarı) gösteriyor.
- **Circular radar**ın en dış menzil halkası **460** gösteriyor.

Aynı dünya kullanılıyorsa circular neden 1000 yerine 460 yazıyor?

## Kısa Cevap

460, aynı dünyanın farklı bir büyüklüğüdür. Üst üste binen **iki neden** var:

1. **Koordinat değil, yarıçap.** Kare radar eksende **dünya koordinatını** (0 → 1000, yani
   tüm kenar uzunluğu) yazıyor. Circular radar ise **merkezden uzaklığı (yarıçap)** yazıyor.
   Merkez (500, 500) olduğu için merkezden kenara olan yarıçap en fazla **500** olabilir
   (1000 çaptır, yarıçap değil). Yani 0.92 katsayısı hiç olmasa bile circular en fazla
   **500** yazardı — 1000 değil.

2. **0.92 kozmetik küçültme.** En dış halka, yarım-dünyanın **%92**'sine çekilmiş:
   `500 × 0.92 = 460`. Bu, çemberin ve etrafındaki açı etiketlerinin (0°–330°) kare
   viewport'a taşmadan sığması içindir.

> Özet: **Kare = koordinat (~1000, tam kenar/çap)**, **Circular = yarıçap × 0.92 (460)**.
> İki sayı farklı şeyleri ölçtüğü için eşit değiller; hata değil, tasarım kaynaklı.

---

## Sayıların Kaynağı (Kod)

### Circular tarafı → 460

`gl/layers/CircularGridLayer.java` (halkalar) ve `gl/layers/CircularLabelLayer.java` (etiket)
aynı formülü kullanır:

```java
float maxRadius = Camera.WORLD_SIZE / 2f * 0.92f;   // 1000/2 * 0.92 = 460
```

Menzil etiketi doğrudan bu yarıçapı yazar (`CircularLabelLayer.java`):

```java
for (float r = 0.25f; r <= 1.0f; r += 0.25f) {
    String lbl = String.valueOf(Math.round(maxRadius * r)); // 115, 230, 345, 460
    ...
}
```

Yani halkalar **115 / 230 / 345 / 460** değerlerinde çizilir.

### Kare tarafı → ~1000

`gl/layers/LabelLayer.java` eksende dünya **koordinatını** yazar; ekran kesirlerindeki
(`GRID_FRACTIONS = {0, 0.25, 0.5, 0.75, 1}`) dünya değerini hesaplar:

```java
float worldX = minX + fraction * (maxX - minX);
String xText = formatLabel(worldX);
```

Varsayılan kamerada `minX = -25`, `maxX = 1025` (dünya 1000 + iki yanda 25 birim boşluk).
Dolayısıyla eksen kabaca **-25 … 500 … 1025** okur; ortası 500, dış kenarı ~1000'dir
(tam 1000 değil, ±25 boşluk payı yüzünden 1025). Kullanıcının gördüğü "1000" bu dünya
kenarıdır.

---

## Önemli Yan Etki: 460 sadece bir etiket değil, gerçek tespit sınırı

`gl/layers/CircularScanLine.java` içinde tarama, merkeze **maxRadius'tan (460) uzak
gemileri hiç tespit etmez**:

```java
float maxRadius = Camera.WORLD_SIZE / 2f * 0.92f;   // 460
...
double distSq = (ex - cx)*(ex - cx) + (ey - cy)*(ey - cy);
if (distSq > maxRadius * maxRadius) continue;        // 460 disi -> atla
```

Sonuçlar:

- Circular radar, 1000 × 1000 dünyanın yalnızca merkezdeki **460 yarıçaplı diskini**
  kapsar. Bu diskin dışında kalan bant ve **dört köşe ölü bölgedir** (hiç taranmaz).
- Bir köşeye en yakın gemi merkeze `√(500² + 500²) ≈ 707` birim uzakta olabilir; bu 460'ın
  çok ötesindedir. Yani dünyanın köşelerindeki gemiler circular radarda **hiç görünmez**.
- Kare radar ise tüm 1000 × 1000 alanı gösterdiği için bu gemileri gösterir.
  İki ekranda **aynı anda görünen gemi sayısı bu yüzden farklı olabilir**.

---

## İki Etiketin Anlamı (özet tablo)

| | Kare Radar (`LabelLayer`) | Circular Radar (`CircularLabelLayer`) |
|---|---|---|
| Ölçülen büyüklük | Dünya **koordinatı** (X ve Y) | Merkezden **yarıçap** (menzil) |
| Eksen aralığı | ~0 – 1000 (tam kenar) | 0 – 460 (= 500 × 0.92) |
| Kapsanan alan | Tüm 1000 × 1000 kare | Merkezdeki 460 yarıçaplı disk |
| En dış etiket | ~1000 | 460 |
| Neden farklı | Koordinat = çap boyu | Yarıçap + %92 kozmetik pay |

---

## Uygulanan Çözüm — Polar (PPI) Eşleme

Yukarıdaki analiz sonrası **circular radar Kartezyen projeksiyondan polar (PPI) yoruma**
geçirildi. Kare radar Kartezyen kaldı; **ham veri (Ship.x / Ship.y) değişmedi** — yalnızca
circular radarın gösterimi değişti.

### Eşleme

```
x -> aci (bearing):   aci    = (x / 1000) * 360°       // 0 = +X ekseni, saatin tersi
y -> menzil (radius): radius = (y / 1000) * maxRadius   // maxRadius = 460
gorsel konum: (cx + radius*cos(aci),  cy + radius*sin(aci)),  cx = cy = 500
```

- Her `(x, y) ∈ [0,1000]²` diskin **içine** düşer → **hiçbir gemi kaybolmaz** (eski köşe ölü
  bölgeleri ortadan kalktı). Tespitteki `distSq > maxRadius²` kırpması kaldırıldı.
- **Menzil etiketleri** `y` verisini gösterir: iç→dış **250 / 500 / 750 / 1000**. Böylece
  circular radar kare radarla **aynı 0–1000 ölçeğini** okur (460 kafa karışıklığı çözüldü).
  Halkaların fiziksel yeri yine `maxRadius*r` (460'a kadar), ama üstündeki sayı `r*1000`.
- **Açı etiketleri** (0°–330°) zaten `x→açı` yönüyle tutarlıydı, değişmedi.

### Merkezîleştirme

Tüm formül tek yerde toplandı: **`gl/core/CircularProjection.java`**
(`maxRadius()`, `angle(x)`, `radius(y)`, `worldX(x,y)`, `worldY(x,y)`). Eskiden 3 dosyada
tekrarlanan `WORLD_SIZE/2 * 0.92` artık buradan gelir.

### Değişen Dosyalar

| Dosya | Değişiklik |
|-------|-----------|
| `gl/core/CircularProjection.java` | **Yeni** — polar eşleme (x→açı, y→menzil) tek merkez |
| `gl/layers/CircularScanLine.java` | Tespit `radius(y)` üzerinden; `maxRadius²` kırpması kaldırıldı (veri kaybı yok) |
| `gl/layers/CircularTargetLayer.java` | Gemi diskteki polar konumuna çizilir; `hitRadius = radius(y)` |
| `gl/layers/CircularLabelLayer.java` | Menzil etiketi `r*1000` (250/500/750/1000) |

> `CircularMinimap` ayrı değişiklik gerektirmedi: blip'lerin `x/y`'sini okuduğundan yeni
> polar konumları otomatik yansıtır.

### Gemi Boyutu Ölçeği (rangeScale)

Konumlar menzil ekseninde `maxRadius/WORLD_SIZE ≈ 0.46` ile sıkıştırıldığından (y=1000 →
yarıçap 460), gemi **boyutları da aynı oranla** ölçeklenir. Aksi halde gemiler 0–1000 etiket
ölçeğinde ~2.17× büyük görünür (konum sıkışıyor ama boyut sıkışmıyor).

- `CircularProjection.rangeScale()` = `maxRadius() / WORLD_SIZE` (= 0.46).
- `CircularTargetLayer`: `arcWidth = max(width * rangeScale, dist * beamAngle)`,
  `thickness = height * rangeScale`.
- `CircularMinimap`: blip boyutu da `* rangeScale`.
- Böylece circular ekran, konum + boyut olarak tekdüze 0.46× ölçeklenmiş tutarlı bir polar
  grafiktir. Kare radar 1:1 kaldığı için orada boyutlar değişmez.
- Radyal **konum** zaten `y * 460/1000`'dir (`CircularProjection.radius`), doğrudan y
  eklenmez.

### Notlar

- Menzil ölçeği (görsel yarıçap = ~yarım dünya) `CircularProjection.maxRadius()` içindeki
  `0.92` ile ayarlanır; kenardaki açı etiketlerine pay bırakmak içindir. Etiket değeri bundan
  bağımsız olarak `1000`'e sabittir.
- `x=0` ve `x=1000` ikisi de 0°'ye denk gelir (bearing periyodiktir); `y=0` merkez,
  `y=1000` en dış halkadır.
- Tarama hâlâ merkezden dışa büyüyen halka (ripple); artık `y` (menzil) ekseninde ilerler,
  yani gemiler kendi menzillerine denk gelen anda parlar.
