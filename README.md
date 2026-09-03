# 🛡️ One UI App Locker for Android

<p align="center">
  <img src="app/src/main/res/drawable/app_logo.jpg" width="128" height="128" alt="One UI App Locker Logo" style="border-radius: 28px;" />
</p>

<p align="center">
  <strong>Samsung One UI 6 & 7 tasarım dilini benimseyen, Clean Architecture + MVVM + Jetpack Compose ile inşa edilmiş, sıfır gecikmeli (0-latency) Android uygulama kilitleyici.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-0381FE?style=for-the-badge" alt="Architecture" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="License" />
</p>

---

## 🌟 Öne Çıkan Özellikler

### 1. 🎨 Gerçek Samsung One UI Deneyimi
- **Viewing Area & Interaction Area:** Üstte geniş, bilgilendirici başlık alanı; altta tek elle rahat erişilebilir etkileşim alanı.
- **Squircle Kart Tasarımı:** One UI standartlarında `26.dp` yumuşak köşeli kartlar ve kapsayıcılar.
- **Fizik Tabanlı Yay Animasyonlu Switch:** Akıcı, dokunsal geçişli One UI anahtarları.
- **Dinamik Tema Sistemi:** 
  - **Sistem Varsayılanı:** Cihazın açık/koyu modunu otomatik takip eder.
  - **Açık Mod & Koyu Mod (One UI Dark):** Manuel seçim desteği.
- **Gizlilik Odaklı İkon:** Mat antrasit-siyah zemin üzerinde fırçalanmış titanyum gümüş kilit simgesi.

### 2. ⚡ Sıfır Gecikme (0-Latency) & Doğal Görev Yığını
- **`AppLockAccessibilityService`:** Pencere değişimlerini (`TYPE_WINDOW_STATE_CHANGED`) sistem seviyesinde olay güdümlü dinleyerek korumalı uygulama henüz ekrana çizilmeden araya girer.
- **Bellek İçi Durum Yönetimi (`AppLockStateHolder`):** Kilitli uygulamaları ve geçici açılmış oturumları bellek içinde $O(1)$ sürede sorgular; ana iş parçacığı veya veritabanı darboğazı oluşturmaz.
- **Doğru Uygulama Yönlendirmesi:** Kilit açıldığında `finish()` ile doğrudan alttaki aktiviteye (Gemini, belirli bir WhatsApp sohbeti veya YouTube videosu) kesintisiz dönüş yapılır; Google Arama'ya atma gibi yönlendirme hataları yaşanmaz.
- **Yedek Koruma:** Erişilebilirlik kapalı olsa dahi pil dostu `UsageStatsManager` servisi devreye girer.

### 3. 🔒 Gelişmiş Güvenlik & Gizlilik
- **Son Uygulamalar (Recents) Gizliliği (`FLAG_SECURE`):** Görev yöneticisinde ve ekran görüntüsü alırken uygulamanın içi tamamen siyah olarak gizlenir.
- **Görev Yöneticisi Koruması:** Kullanıcı son uygulamalar menüsüne geçtiği an aktif kilit oturumu sıfırlanır; karttan geri dönüldüğünde anında kilit ekranı araya girer.
- **Esnek Yeniden Kilitleme:**
  - *Anında (Uygulamadan çıkıldığı an)*
  - *Ekran kapandığında*
  - *1 dakika sonra*
  - *5 dakika sonra*
- **Android Keystore & Biyometri:** Tuzlu SHA-256 şifreleme ve `BiometricPrompt` ile parmak izi/yüz tanıma.

---

## 🏗️ Mimari ve Proje Yapısı

Proje, **Clean Architecture** prensiplerine ve ayrık sorumluluklara (Separation of Concerns) göre modüler paketlenmiştir:

```
OneUi_applocker/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── res/
│   │   │   ├── drawable/               # One UI vektör ikonları & adaptif launcher
│   │   │   ├── values/                 # strings.xml, colors.xml, themes.xml
│   │   │   └── xml/                    # Accessibility & backup yapılandırmaları
│   │   └── java/com/oneui/applocker/
│   │       ├── AppLockerApp.kt         # Application sınıfı ve bağımlılık yöneticisi
│   │       ├── core/
│   │       │   ├── theme/              # Color, Shape, Type, OneUiAppLockerTheme
│   │       │   ├── designsystem/       # OneUiHeader, OneUiCard, OneUiSwitch, OneUiKeypad
│   │       │   ├── security/           # SecurityManager, BiometricHelper, AppLockStateHolder
│   │       │   └── permission/         # PermissionHelper, PermissionType
│   │       ├── data/
│   │       │   ├── model/              # AppItem, LockSettings, ThemeMode, RelockPolicy
│   │       │   ├── database/           # Room DB: AppDatabase, LockedAppDao, LockedAppEntity
│   │       │   └── repository/         # AppRepository, SettingsRepository (DataStore)
│   │       ├── service/
│   │       │   ├── AppLockAccessibilityService.kt   # Sıfır gecikmeli olay dinleyici
│   │       │   ├── AppMonitorForegroundService.kt   # Yedek arka plan koruyucu
│   │       │   └── BootCompletedReceiver.kt         # Cihaz açılışında otomatik başlatıcı
│   │       └── ui/
│   │           ├── MainActivity.kt     # Ana etkinlik ve navigasyon kapsayıcısı
│   │           ├── navigation/         # AppNavHost, Screen rotaları
│   │           ├── home/               # Uygulama listesi ve kilit aç/kapa
│   │           ├── lock/               # Kilit ekranı (PIN, Desen, Biyometrik)
│   │           ├── permissions/        # Adım adım izin onay ekranı
│   │           └── settings/           # Kilit türü, yeniden kilitleme, tema ayarları
```

---

## 📦 APK Olarak Dışarı Aktarma (Export APK)

Uygulamanızı telefonunuza yüklemek veya arkadaşlarınızla paylaşmak için APK dosyasını 2 şekilde üretebilirsiniz:

### Yöntem 1: Android Studio ile Hızlı Debug APK
1. Android Studio'da projeyi açın.
2. Üst menüden: **Build > Build Bundle(s) / APK(s) > Build APK(s)** seçeneğine tıklayın.
3. Derleme tamamlandığında sağ alt köşede beliren bildirimde **"locate"** butonuna tıklayın.
4. APK dosyanız hazırdır:
   `app/build/outputs/apk/debug/app-debug.apk`

### Yöntem 2: Dağıtım İçin İmzalı (Signed Release) APK
1. Android Studio'da: **Build > Generate Signed Bundle / APK...** menüsüne girin.
2. **APK** seçeneğini işaretleyip **Next**'e basın.
3. Bir anahtar dosyası (Key store path) seçin veya **Create new...** ile yeni bir anahtar oluşturun.
4. Build Variants kısmında **release** seçin ve **Finish**'e tıklayın.
5. Oluşan optimize edilmiş dosya:
   `app/release/app-release.apk`

---

## 🚀 GitHub'da Paylaşma Adımları

### 1. Yerel Git Deposunu Başlatma ve Commit
Terminali (veya PowerShell'i) proje klasöründe açıp şu komutları sırasıyla çalıştırın:

```bash
# Git deposunu başlatın
git init

# Dosyaları ekleyin (.gitignore gereksiz dosyaları otomatik hariç tutar)
git add .

# İlk commit'i yapın
git commit -m "feat: Initial commit - One UI App Locker with Compose & Zero Latency"
```

### 2. GitHub'da Depo Açıp Yükleme (Push)
1. [GitHub](https://github.com/new) adresine gidin.
2. Repository name olarak `OneUi_applocker` yazın ve **Public** seçin (README veya .gitignore eklemeyin, projede zaten hazır).
3. **Create repository** butonuna basın.
4. Terminalde aşağıdaki komutları çalıştırın (kendi kullanıcı adınızı yazın):

```bash
git branch -M main
git remote add origin https://github.com/<KULLANICI_ADINIZ>/OneUi_applocker.git
git push -u origin main
```

### 3. APK'yı GitHub'da İndirilebilir Hale Getirme (Releases)
1. GitHub deponuzun sağ tarafındaki **"Releases"** bölümüne gidin.
2. **"Draft a new release"** (Yeni sürüm oluştur) butonuna tıklayın.
3. **Tag version:** `v1.0.0` yazın.
4. **Release title:** `One UI App Locker v1.0.0 - İlk Kararlı Sürüm` yazın.
5. Alt kısımdaki kutucuğa ürettiğiniz `.apk` dosyasını sürükleyip bırakın.
6. **"Publish release"** butonuna basın. Artık herkes APK'yı doğrudan indirebilir!

---

## 📄 Lisans

Bu proje [MIT Lisansı](LICENSE) altında lisanslanmıştır.
