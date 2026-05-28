# 🌍 GeoField Pro — Industrial Geological Field Companion
### *Professional Industrial-Grade Field Logging Terminal for Geological Surveying & Exploration*

[![Android Build CI](https://github.com/farzadf/geofield-pro/actions/workflows/android.yml/badge.svg)](https://github.com/farzadf/geofield-pro/actions/workflows/android.yml)
![Kotlin Version](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)
![Jetpack Compose](https://img.shields.io/badge/Compose-M3-green.svg)
![Database](https://img.shields.io/badge/Database-Room_SQLite-orange.svg)
![Version](https://img.shields.io/badge/App_Version-1.1-red.svg)

---

###  معرفی اجمالی پروژه برنامه ثبت داده‌های زمین‌شناسی صحرایی و معدنی بر پایه معماری مدرن اندروید است. این اپلیکیشن به صورت کاملاً آفلاین (Offline-First) طراحی گردیده و شرایط متداول محیط‌های اکتشافی سخت (نور شدید خورشید و مانیتورینگ متناوب ماهواره‌ها) را پوشش می‌دهد. در نسخه **۱.۱**، تمرکز فوق‌العاده‌ای بر بریدج‌سازی و صدور اطلاعات به محیط‌های دسکتاپ نظیر **ArcGIS**، **QGIS** و دیتابیس‌های مرکزی معدن گذاشته شده است.

---

## ⚡ Key Updates & Advancements in Version 1.1

The **v1.1 Release** of GeoField Pro bridges the gap between raw physical field data observation and professional office GIS post-processing pipelines.

### 1. Robust GIS ZIP Package Exporter (`exportProjectZip`)
The export stack has been expanded from a single spreadsheet to a comprehensive, structural **GIS ZIP Package**. This allows instantaneous synchronization with industry-standard geology environments:
- **Relational Datasets (CSV Paths under `data/`):** Auto-generated, normalized, comma-separated datasets (`stations_dataset.csv`, `lithologies_dataset.csv`, `alterations_dataset.csv`, `structures_dataset.csv`, `samples_dataset.csv`) ready for drag-and-drop ingestion into **QGIS, ArcGIS, Leapfrog,** or custom database servers.
- **Relational Field Assets Folder (`field_photos/`):** Matches the UUID mappings inside database sheets with actual physical geotagged photographs, saving hours of manual file mapping.
- **Master Excel Book (`Geology_Logs_Workbook.xlsx`):** A multi-sheet master spreadsheet displaying hierarchical logs with calculated UTM transformations.

### 2. High-Contrast Material 3 Export Selection Modal
A responsive selection modal has been designed adhering to modern design layouts:
- Offers intuitive selection between a quick **Excel Workbook (.xlsx)** download and the complete **GIS ZIP Package**.
- Styled with dedicated Material Design 3 state indications, offering easy-to-use tap target layouts suitable for one-handed operation or rugged gloves.

### 3. Native GitHub Actions Continuous Integration (`android.yml`)
Integrated an automated cloud-based Gradle runner to guarantee every target commit and merge request compiles reliably on Java 17, validating build outputs and syntax errors proactively.

---

## 🌟 Core System Specifications

### 📍 Multi-System Geolocation Engine (WGS84 & UTM Grid)
- Tracks live spatial parameters, converting coordinates on-the-fly from ellipsoidal **WGS84 (Lat/Long)** to **UTM (Universal Transverse Mercator)** projected coordinate systems without requiring internet connectivity.
- Direct UTM Zone calculation, easting, northing, and accuracy status indications using dynamic high-visibility indicators.

### 🪨 Hierarchical Logging Architecture
Provides highly structured forms to capture physical ground truths:
- **Lithology:** Fast descriptor choices representing textures, classifications, colors, grain sizes, mineralization, and alteration properties.
- **Alteration:** Quantitative zones categorizing intensities, associated minerals, host rock types, and relevant notes.
- **Faults & Structures:** Precision strike/dip calculations and structure type tags (Joint, Vein, Fault plane tracking).
- **Physical Samples:** Rapidly registers rock/core inventory samples to unique parent observation stations.

### 📸 Geo-Referenced Augmented Camera Terminal
- An integrated camera view draws orientation properties, WGS84 coordinates, UTM coordinates, and exact timestamps directly onto captured imagery.
- Automatically packs, compresses, and references photo paths relative to logged geological segments.

---

## 📐 Design Tokens & Visual Hierarchy
Optimized strictly for outdoor high-glare setups with high contrast ratios (> 7:1) using clear typography.

| Token | Hex Value | Application Placement |
|---|---|---|
| **BACKGROUND** | `#F4F1E8` | High-readability canvas simulating natural field lithology sheets. |
| **SURFACE** | `#FFFFFF` | Isolated UI items, interactive cards, structure menus. |
| **PRIMARY_TEXT** | `#101010` | 100% Contrast level for metric titles, UTM data coordinates. |
| **SECONDARY_TEXT**| `#4F545A` | Intermediate description annotations, metadata tags, indicators. |
| **BORDER** | `#D8D2C6` | Clean framing outlines separating categories cleanly. |

---

## 🛠️ Architecture & Core Pipeline

```
┌────────────────────────────────────────────────────────┐
│                        UI Layer                        │
│     (Jetpack Compose - Inter / IBM Plex Mono Fonts)    │
└───────────┬────────────────────────────────┬───────────┘
            │                                │
            ▼                                ▼
┌───────────────────────┐        ┌───────────────────────┐
│     MainViewModel     │◄───────┤    PhotoProcessor     │
│  (StateFlow Flux Core) │        │ (Exif/Watermark Draw) │
└───────────┬───────────┘        └───────────────────────┘
            │
            ▼
┌────────────────────────────────────────────────────────┐
│                    Repository                          │
│       (GeoFieldDatabase Room SQLite Wrapper)          │
└────────────────────────────────────────────────────────┘
```

---

## 📁 Repository Map

```
├── .github
│   └── workflows
│       └── android.yml            # Automated CI Build Actions Pipeline (v1.1)
├── app
│   ├── build.gradle.kts           # Gradle Module-level properties and dependency chains
│   └── src
│       └── main
│           ├── AndroidManifest.xml # Core hardware permissions registers (Location, Camera, Sensors)
│           ├── java
│           │   └── com
│           │       └── example
│           │           ├── MainActivity.kt        # Primary view structures, Dialog layers and Navigation
│           │           ├── ViewModel.kt           # ViewModel, StateFlow flows, and ZIP exporter
│           │           ├── Database.kt            # Room Persistent DB, Schemas, DAO interfaces
│           │           ├── PhotoProcessor.kt      # Georeference Image Pipeline
│           │           ├── UtmConverter.kt        # Ellipsoidal WGS84 to UTM Transverse Mercator
│           │           ├── XlsxWriter.kt          # Offline XLSX Spreadsheet Builder
│           │           └── ui
│           │               └── theme
│           │                   ├── Color.kt       # Unified color palette definition
│           │                   ├── Theme.kt       # Material 3 Custom palette injection
│           │                   └── Type.kt        # Type tokens with monospace scale pairings
```

---

## 🚀 Workstation Compilation & Setup

To compile and verify the project locally:

### Prerequisites
* **Android Studio Ladybug (2024.2.1) or higher**
* **Java Development Kit (JDK) 17**
* **Gradle Build Tool 8.4+**

### Compilation Steps

1. **Clone the Source Code**
   ```bash
   git clone https://github.com/farzadf/geofield-pro.git
   cd geofield-pro
   ```

2. **Run Dependency Synthesis & Local Compiles**
   ```bash
   gradle assembleDebug
   ```
   This compiles and packs the diagnostic, fully signed APK directly to `app/build/outputs/apk/debug/`.

3. **Required Hardware Permissions**
   Verify your test environment allows runtime checks for:
   * `ACCESS_FINE_LOCATION` (Required to compute projected coordinate metrics)
   * `CAMERA` (Required for spatial overlay watermarked photography)

---

*GeoField Pro is engineered with absolute resilience at its core. Built for extreme environments, delivering surveyor-grade accuracy wherever the exploration leads.*
