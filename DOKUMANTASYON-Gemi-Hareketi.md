# Gemi Hareketi + Çarpışma + Radar Dondurma — Değişiklik Dokümantasyonu

Tarih: 2026-07-20

## İstenen

1. Gemiler hareket etsin.
2. Gemiler **birbirleriyle çarpışmasın** (içine geçmesin).
3. Bir gemi radarda **en son görüldüğü yerde kalsın**; scanline geminin o anki (gerçek)
   konumuna değdiğinde blip'in yeri güncellensin.

## Yaklaşım

Simülasyon ile gösterim ayrıştırıldı:

- **Simülasyon** (gerçek konum): gemiler sürekli hareket eder, duvarlardan seker, birbirleriyle
  çarpışıp ayrılır. Konum her tick güncellenir ve **çakışmasız** tutulur.
- **Gösterim** (blip): radar, gemiyi **en son tarandığı** konumda gösterir (donmuş blip).
  Scanline geminin güncel konumuna değdiğinde blip o konuma sıçrar. Bu "dondurma" mekanizması
  `TargetLayer` / `CircularTargetLayer` içinde zaten vardı — gemiler sabitken fark edilmiyordu;
  hareketle birlikte anlamlı hale geldi (kod değişmedi).

## Uygulama

### Hareket — `Ship.move(deltaTime)`
- Konum `position += velocity * dt` ile ilerletilir (delta-time bazlı, FPS'den bağımsız).
- **Duvar sekmesi:** gemi merkezi kenardan yarım gemi boyu içeride kalır; sınıra değince konum
  sabitlenir ve ilgili hız bileşeni ters çevrilir (`Vector2D.reflectX/reflectY`). Böylece
  geminin hiçbir kısmı matris dışına taşmaz.
- Hız büyüklüğü spawn'da `[minShipSpeed, maxShipSpeed]` aralığında atanır (config).

### Gemi-Gemi Çarpışma — `SimulationEngine`
Tick sırası: `spawnMissingShips → updateAllEntities (hareket) → resolveCollisions → removeDead`.

`resolveCollisions()`:
- Tüm gemi çiftleri için AABB çakışması kontrol edilir.
- Çakışan çift **en az girişim ekseninde** (x veya y) ayrılır: her gemi çakışmanın yarısı
  kadar zıt yönde itilir (sınır içine kırpılarak).
- O eksendeki **hız bileşenleri takas edilir** (eşit kütle elastik çarpışma) → gemiler
  birbirinden uzaklaşır.
- Aynı anda çoklu çakışmalar için pas 2 kez tekrarlanır (`COLLISION_ITERATIONS = 2`).

### Ek
- `Ship.setVelocity(Vector2D)` eklendi (çarpışma yanıtı motordan hızı günceller).

## Değişen Dosyalar

| Dosya | Değişiklik |
|-------|-----------|
| `sim/model/Ship.java` | `move()` gerçek hareket + duvar sekmesi; `setVelocity()` eklendi |
| `sim/engine/SimulationEngine.java` | Tick'e `resolveCollisions()`; `resolvePair()`, `clampToBounds()` eklendi |

> `TargetLayer` / `CircularTargetLayer` / scanline'lar **değişmedi**; dondurma davranışı
> mevcut `detected` → blip güncelleme mantığından gelir.

## Doğrulama

Headless test (60 gemi, hız 40–90 px/s, ~4 sn, yoğun çarpışma):

- Hareket eden gemi: **60/60**.
- Her örnekleme anında **sınır dışı = 0**, **çakışma = 0**.

## Notlar

- Radarda iki **donmuş** blip görsel olarak üst üste görünebilir; bunlar farklı zamanlardaki
  taranmış anlık konumlardır. Gerçek (simülasyon) konumlar hiçbir anda çakışmaz — bu, radar
  dondurma davranışının doğal (ve gerçekçi) sonucudur.
- Çarpışma çözümü AABB (kutu) tabanlıdır; Kartezyen simülasyon matrisinde geçerlidir. Çok
  yoğun köşe yığılmalarında 2 pas altında sub-piksel artık girişim kalabilir, sonraki tick'te
  temizlenir (görsel etkisi yok).
