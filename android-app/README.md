# Robot Controller Android Uygulaması

Bu uygulama, HC-06 Bluetooth modülü üzerinden Arduino tankını kontrol etmek için hazırlandı.

## Özellikler
- HC-06 ile eşleşmiş cihazlara bağlanır.
- İleri, geri, sağ, sol komutlarını gönderir.
- Tuş bırakıldığında otomatik `S` (dur) komutu yollar.
- Arduino'nun gönderdiği mesafe loglarını ekranda gösterir.

## Kurulum
1. Android Studio ile `android-app` klasörünü açın.
2. Telefonda HC-06 ile eşleştirme yapın (1234 / 0000).
3. Uygulamayı çalıştırın ve **Bağlan (HC-06)** butonuna basın.

## Komutlar
- **İleri:** `F`
- **Geri:** `B`
- **Sol:** `L`
- **Sağ:** `R`
- **Dur:** `S`
