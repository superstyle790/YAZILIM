# Robot Controller Android Uygulaması

Bu uygulama, HC-06 Bluetooth modülü üzerinden Arduino tankını kontrol etmek için hazırlandı.

## Özellikler
- HC-06 ile eşleşmiş cihazlara bağlanır.
- Tek bir joystick alanı üzerinden ileri, geri, sağ, sol komutlarını gönderir.
- Joystick bırakılınca veya merkezdeyken otomatik `S` (dur) komutu yollar.
- Arduino'nun gönderdiği mesafe loglarını ekranda gösterir.

## Kurulum
1. Android Studio ile `android-app` klasörünü açın.
2. İlk açılışta Gradle senkronizasyonunu tamamlayın.
3. Telefonda HC-06 ile eşleştirme yapın (1234 / 0000).
4. Uygulamayı çalıştırın ve **Bağlan (HC-06)** butonuna basın.

## APK/Android Uygulamasına Dönüştürme
Bu proje zaten bir Android Studio projesidir. Uygulamayı APK olarak almak için:
1. Android Studio'da üst menüden **Build > Build Bundle(s) / APK(s) > Build APK(s)** seçin.
2. Oluşan APK'yı **Event Log** içinden açabilir veya `android-app/app/build/outputs/apk/debug/app-debug.apk` yolundan alabilirsiniz.
3. APK'yı telefona kopyalayıp yükleyin (gerekirse "Bilinmeyen kaynaklar" izni verin).

> Not: Play Store için "Signed APK/AAB" üretmeniz gerekir. Bunun için **Build > Generate Signed Bundle / APK** menüsünü kullanın.

## Komutlar
- **İleri:** `F`
- **Geri:** `B`
- **Sol:** `L`
- **Sağ:** `R`
- **Dur:** `S`
