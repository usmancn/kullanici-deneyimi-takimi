# Çakışmasız Gemi Yerleşimi — Değişiklik Dokümantasyonu

Tarih: 2026-07-20

## İstenen

1000×1000'lik simülasyon alanını bir **matris** gibi düşünüp gemileri (gain factor'leriyle
birlikte) öyle yerleştirmek ki:

1. Gemiler **birbirinin içine geçmesin** (her gemi kendi `width × height` alanını kaplar).
2. Geminin **hiçbir kısmı matris dışına taşmasın**.

## Model

- Bir geminin **konumu, gövdesinin merkezidir**. `width × height` boyutuyla gemi
  `[x − w/2, x + w/2] × [y − h/2, y + h/2]` eksen-hizalı dikdörtgenini (AABB) kaplar.
- **Sınır kuralı:** `x − w/2 ≥ 0`, `x + w/2 ≤ 1000`, `y − h/2 ≥ 0`, `y + h/2 ≤ 1000`.
- **Çakışma kuralı:** iki geminin AABB'si kesişmesin. Merkezler için:
  ```
  |ax − bx| < (aw + bw)/2   VE   |ay − by| < (ah + bh)/2   →  çakışma
  ```

Not: Bu kurallar Kartezyen simülasyon matrisinde (kare radarın gerçek uzayı) geçerlidir.
Circular radar aynı veriyi polar gösterdiği için ekranda görsel örtüşme olabilir; bu bir
gösterim etkisidir, matriste çakışma yoktur.

## Uygulama

Çakışma kontrolü mevcut gemileri gerektirdiğinden yerleştirme **`SimulationEngine`**'de yapılır
(rastgele deneme / rejection sampling):

```
spawnMissingShips():
  mevcut < max değilse çık
  placed = mevcut gemiler
  her eksik gemi için:
    ship = new Ship(config)                 # w/h/gain atanır
    placeWithoutOverlap(ship, placed) ?
        -> entityManager'a ekle, placed'e ekle
        -> bulunamazsa break (bu tick vazgeç, sonraki tick tekrar dene)

placeWithoutOverlap:
  önce yapıcıdan gelen konumu dene
  olmazsa placementMaxAttempts kadar rastgele (sınır-güvenli) aday dene
  uygun bulunca setPosition

fitsAt: sınır kontrolü + tüm mevcut gemilerle AABB çakışma kontrolü
```

- `Ship.spawnRandomPosition()` artık merkezi kenarlardan en az yarım gemi boyu içeride üretir
  (sınır-güvenli başlangıç adayı).
- `placementMaxAttempts` (varsayılan **200**) `SimulationConfig`'e eklendi; alan dolduğunda
  sonsuz döngüyü önler.
- Gemiler hareketsiz olduğundan yerleşim **bir kez** (açılışta) yapılır ve kalıcıdır.

## Değişen / Eklenen Dosyalar

| Dosya | Değişiklik |
|-------|-----------|
| `sim/engine/SimulationEngine.java` | Çakışmasız yerleştirme: `spawnMissingShips` yeniden yazıldı; `placeWithoutOverlap`, `fitsAt`, `randomBoundedPosition`, `aabbOverlap`, `collectShips` eklendi |
| `sim/model/Ship.java` | `spawnRandomPosition()` sınır-güvenli hale getirildi (merkez ± yarım boy içeride) |
| `config/SimulationConfig.java` | `placementMaxAttempts` (varsayılan 200) + getter/setter |

## Doğrulama

Gerçek sınıflarla (motor + gemi, GL'siz) headless testler çalıştırıldı:

- **1000×1000, 200 gemi:** hepsi yerleşti → sınır dışı 0, çakışma 0.
- **200×200 (dar alan), 100000 gemi:** 175 gemi sığdı, sınır dışı 0, çakışma 0; motor
  **güvenli durdu** (donma/çökme yok).

## Notlar

- Alan doyduğunda motor her tick kalan (sığmayan) gemiler için tekrar
  `placementMaxAttempts` deneme yapar. Gerçek kullanımda (1000×1000'de 20–50 gemi) bu durum
  hiç oluşmaz; hepsi anında yerleşir ve `current ≥ max` erken çıkışı devreye girer. Çok büyük
  `maxShipCount` verildiğinde bu tekrar denemeler küçük bir CPU maliyeti oluşturur.
- Kenarların tam değmesi (teğet) çakışma sayılmaz; istenirse `fitsAt` içine küçük bir güvenlik
  payı (margin) eklenebilir.
