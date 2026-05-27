# 🌍 GeoField Pro — Industrial Geological Field Companion
### *Bilingual Technical Documentation & GitHub Repository Guide (فارسی | English)*

---

## 🇮🇷 راهنمای جامع و مستندات فنی پروژه (Persian Introduction)

**GeoField Pro** یک اپلیکیشن موبایل صنعتی نسل جدید، با طراحی به سبک ابزارهای مهندسی اپل، مخصوص ثبت داده‌های زمین‌شناسی، معدن و اکتشاف صحرایی است. این نرم‌افزار برای کارکرد در شرایط سخت (نور مستقیم خورشید، گرد و غبار، و آفلاین مطلق) بهینه شده و به جای یک نرم‌افزار مصرفی معمولی، حس یک **ابزار اندازه‌گیری آزمایشگاهی دقیق** را به مهندس زمین‌شناس انتقال می‌دهد.

### 🌟 ویژگی‌های کلیدی سیستم
1. **طراحی صنعتی بهینه‌شده برای نور خورشید:** استفاده از کنتراست بالای رنگی (> 7:1) و تایپوگرافی واضح فونت‌های **Inter** و **IBM Plex Mono** جهت خوانش سریع مختصات در کسری از ثانیه زیر شدیدترین تابش‌های بیابانی.
2. **سیستم موقعیت‌یابی آفلاین هوشمند:** نمایش لحظه‌ای مختصات دقیق جغرافیایی (WGS84) و تبدیل آنی به کدهای سیستم تصویر یوتی‌ام (**UTM Grid**)، همراه با پایش مداوم دقت خطای GPS با کدهای رنگی داینامیک.
3. **ثبت جامع ساختارهای زمین‌شناسی:**
   - **Lithology:** فرم‌های هوشمند انتخاب جنس و توصیف سنگ‌ها با کمترین نیاز به کیبورد.
   - **Alteration:** زون‌های دگرسانی با قابلیت انتخاب گزینه‌ای سریع.
   - **Structural Readings:** اندازه‌گیری و ثبت شیب (Dip)، امتداد شیب (Dip Direction/Strike) به صورت دستی و با سنسور.
   - **Samples:** لاگ نمونه‌های فیزیکی سنگ با برچسب‌گذاری شناسه بارکد مانند.
4. **دوربین واقعیت افزوده و واترمارک اطلاعات مکانی (Geo-Referenced Camera):** دوربین داخلی هوشمند که هنگام ثبت تصاویر صحرایی، زاویه انحراف قطب‌نما (Azimuth)، زمان دقیق، موقعیت UTM، طول و عرض جغرافیایی را مستقیماً روی تصویر ذخیره کرده و به عنوان سند اکتشاف پکیج می‌کند.
5. **تهیه گزارش در قالب اکسل چندبرگه اکسل (XLSX Compiler):** خروجی کاملاً آفلاین گزارشات منطبق با قالب‌های بین‌المللی اکتشاف برای بارگذاری مستقیم در دیتابیس‌های مرکزی معدن.

---

## 🇺🇸 Technical Specification Prompt & Architectural Overview (English)

`GeoField Pro` is engineered with an **offline-first MVVM design pattern** in pure **Kotlin and Jetpack Compose**. It is structured as a resilient, high-contrast, professional-grade logging terminal.

---

## 📐 Design System & Color Tokens
In strict alignment with the premium industrial UI design tokens, the application enforces high contrast ratios to guard against glare and heavy field dust.

| Design Token | Hex Code | System Allocation |
|---|---|---|
| **BACKGROUND** | `#F4F1E8` | Base application canvas representing natural field lithology tone. |
| **SURFACE** | `#FFFFFF` | Structured cards, input fields, and distinct UI container backings. |
| **PRIMARY_TEXT** | `#101010` | 100% contrast core readable text and metric values. |
| **SECONDARY_TEXT** | `#4F545A` | Medium-contrast structural labels, metadata, and icons. |
| **BORDER** | `#D8D2C6` | Clean framing lines to organize sections without creating visual noise. |

### 🚨 Action Colors (Function-Based States)

*   🔵 **PRIMARY_ACTION** (`#1E5EFF`): Utilized strictly for core high-level calls to action (e.g., *Add Station*, *Save Observation*, *Export Project*).
*   🟢 **GPS_ACTIVE** (`#00A86B`): Indicates high precision location tracking and active coordinate logs.
*   🟡 **DATA_WARNING** (`#FFB020`): Triggered by low satellite visibility, high elevation errors, or warnings.
*   🔴 **DATA_ERROR** (`#FF3B30`): Urgent warnings such as lost connection, camera-sensor failure, or structural omissions.
*   🟢 **SUCCESS** (`#2ECC71`): Provides reassurance on local DB persistence commits and Excel compiles.

---

## 🛠️ Architecture & Core Components

```
┌────────────────────────────────────────────────────────┐
│                     UI Layer                          │
│     (Jetpack Compose - Inter / IBM Plex Mono Type)     │
└───────────┬────────────────────────────────┬───────────┘
            │                                │
            ▼                                ▼
┌───────────────────────┐        ┌───────────────────────┐
│     MainViewModel     │◄───────┤    PhotoProcessor     │
│  (StateFlow Flow engines)     │        │ (Exif/Watermark Draw) │
└───────────┬───────────┘        └───────────────────────┘
            │
            ▼
┌────────────────────────────────────────────────────────┐
│                    Repository                          │
│       (GeoFieldDatabase Room SQLite Wrapper)          │
└────────────────────────────────────────────────────────┘
```

### 1. Unified State Engine (`ViewModel.kt`)
Holds immutable reactive flows representing geographic coordinate variations, live sensor data (orientation matrix for compass readings), active validation profiles, and local project schemas.

### 2. High-Performance Spatial Image Matrix (`PhotoProcessor.kt`)
Draws high-contrast instrumentation details directly on the canvas preview to export images embedded with EXIF headers and custom geological watermarks.

### 3. Native Sensors Wrapper
Subscribes to Android's `Sensor.TYPE_ROTATION_VECTOR` or combination `TYPE_ACCELEROMETER` + `TYPE_MAGNETIC_FIELD` to calculate real-time magnetic Azimuth (0° to 360°) and strike orientations.

### 4. Seamless SQLite Core (`Database.kt`)
Implements **Room Database Entity-Models** representing geological data relational structures.
*   **Projects Schema:** `id`, `name`, `stationCount`, `createdAt`.
*   **Stations Schema:** `id`, `projectId`, `code`, `latitude`, `longitude`, `utmZone`, `utmEasting`, `utmNorthing`, `accuracy`, `elevation`, `timestamp`.
*   **Observations Schema:** `id`, `stationId`, `lithologyCode`, `lithologyDescription`, `alterationZone`, `structureType`, `measurementText` (Dip/Dip Direction), `hasSample`, `sampleCode`.

---

## 📁 Repository Directory Structure

```
├── app
│   ├── build.gradle.kts           # Module properties & compiler configurations
│   └── src
│       └── main
│           ├── AndroidManifest.xml # Hardware permissions (Location, Sensors, Camera)
│           ├── java
│           │   └── com
│           │       └── example
│           │           ├── MainActivity.kt        # View / Navigation Controller & Compose UI Elements
│           │           ├── ViewModel.kt           # Business Rules Coordinator
│           │           ├── Database.kt            # Jetpack Room Local Storage Layer
│           │           ├── PhotoProcessor.kt      # Image EXIF Watermarking Pipeline
│           │           ├── UtmConverter.kt        # WGS84 Ellipsoid to UTM Coordinates Engine
│           │           ├── XlsxWriter.kt          # Offline Excel Spreadsheet Compiler
│           │           └── ui
│           │               └── theme
│           │                   ├── Color.kt       # Industrial Color Tokens
│           │                   ├── Theme.kt       # Material 3 Custom Theme Configuration
│           │                   └── Type.kt        # Typography (IBM Plex Mono + Inter Scale)
```

---

## 🚀 Local Build & Deployment Guidelines

To build and compile GeoField Pro on your workstation:

### Prerequisites
*   **Android Studio Ladybug (2024.2.1) or higher**
*   **Java SE Development Kit (JDK) 17**
*   **Grade 8.4+**

### Step-by-Step Compilation

1.  **Clone the Repository**
    ```bash
    git clone https://github.com/username/geofield-pro.git
    cd geofield-pro
    ```

2.  **Add Your Environment Variables or Local Keys**
    If implementing external mapping API layers, specify them in the workspace `.env` or project settings panel in Android Studio.

3.  **Run Compilation Checks using Command-Line**
    ```bash
    gradle assembleDebug
    ```
    This generates the diagnostic and testable APK under `app/build/outputs/apk/debug/`.

4.  **Hardware Permissions Configuration**
    Ensure your hardware device grants these critical runtime permissions:
    *   `android.permission.ACCESS_FINE_LOCATION` (GPS Precision Lock)
    *   `android.permission.CAMERA` (Geo-referenced Watermarked Imagery)

---

## 📊 Industrial Excel Export Structure (XLSX Schema)

The generated Excel output from `/XlsxWriter.kt` consists of highly structured, machine-readable tabs designed for direct integration in software like ArcMap, QGIS, Leapfrog, or MineSight:

1.  **STATIONS TAB:** Core station coordinates, altitude, UTM components, precision ratings, logging times.
2.  **LITHOLOGY & ALTERATION TAB:** Geologic units, primary minerals, alteration style, alteration intensity.
3.  **STRUCTURES TAB:** Structural planes, fault descriptors, Dip angle, Dip Direction, Strike measurements.
4.  **SAMPLES TAB:** Sample IDs, rock type descriptions, physical packaging details.

---

*GeoField Pro is built for extreme conditions where reliability isn't a feature; it's a survival requirement. Engineered for geologists, by geologists.*
