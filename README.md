# 📱 Buscando a Dios - App Android

## ✅ Proyecto listo para compilar

Esta es una app WebView mejorada que carga `https://buscandoadios-espana.com/`

### 📋 Características incluidas:
- ✅ Splash screen con tu logo (2.5 segundos)
- ✅ WebView a pantalla completa
- ✅ Detecta si no hay internet → muestra mensaje amigable
- ✅ Botón "atrás" navega dentro de la web
- ✅ Guarda sesión (cookies persistentes)
- ✅ Permite descargar archivos (PDFs, etc.)
- ✅ Permite subir fotos (para perfil, etc.)
- ✅ Abre enlaces externos en navegador (WhatsApp, teléfono, email)
- ✅ Deslizar hacia abajo para actualizar
- ✅ Barra de estado con color dorado
- ✅ Icono personalizado con tu logo

---

## ⚠️ NOTA IMPORTANTE

El archivo `gradle-wrapper.jar` no está incluido. Se descargará automáticamente cuando uses GitHub Actions (Opción 1) o Gitpod (Opción 3).

---

## 🔧 OPCIÓN 1: Compilar con GitHub Actions (Recomendado - Gratis)

### Paso 1: Crear cuenta en GitHub
1. Ve a https://github.com/signup
2. Crea una cuenta gratuita

### Paso 2: Subir el proyecto
1. Crea un nuevo repositorio
2. Sube todo el contenido de la carpeta `BuscandoADios`

### Paso 3: Usar GitHub Actions para compilar
1. En tu repositorio, ve a "Actions"
2. Crea un nuevo workflow con este contenido:

```yaml
name: Build APK

on:
  push:
    branches: [ main ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build Debug APK
      run: ./gradlew assembleDebug

    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

3. La APK se generará automáticamente y podrás descargarla desde "Artifacts"

---

## 🔧 OPCIÓN 2: Compilar con APPETIZE.IO + EXPO

(Más sencillo pero menos control)

1. Ve a https://appetize.io
2. Sube el proyecto como ZIP
3. Genera la APK directamente

---

## 🔧 OPCIÓN 3: Usar Android Studio Online (Gitpod)

1. Ve a https://gitpod.io
2. Conecta tu repositorio de GitHub
3. Abre el proyecto en Gitpod
4. Ejecuta: `./gradlew assembleDebug`
5. Descarga la APK de `app/build/outputs/apk/debug/`

---

## 📁 Estructura del proyecto

```
BuscandoADios/
├── app/
│   ├── src/main/
│   │   ├── java/com/buscandoadios/espana/
│   │   │   ├── MainActivity.java      # WebView principal
│   │   │   └── SplashActivity.java    # Pantalla de inicio
│   │   ├── res/
│   │   │   ├── layout/                # Diseños XML
│   │   │   ├── values/                # Colores, textos, temas
│   │   │   ├── drawable/              # Logo splash
│   │   │   ├── mipmap-*/              # Iconos de la app
│   │   │   └── xml/                   # Configuración de red
│   │   └── AndroidManifest.xml        # Configuración de la app
│   ├── build.gradle                   # Dependencias del módulo
│   └── proguard-rules.pro             # Optimización
├── build.gradle                       # Configuración global
├── settings.gradle
├── gradle.properties
└── gradle/wrapper/
```

---

## ⚙️ Personalización

### Cambiar la URL:
Edita `MainActivity.java` línea 44:
```java
private static final String WEB_URL = "https://TU-NUEVA-URL.com/";
```

### Cambiar colores:
Edita `res/values/colors.xml`

### Cambiar nombre de la app:
Edita `res/values/strings.xml`

---

## 📱 Versiones soportadas
- **Mínimo:** Android 7.0 (API 24)
- **Objetivo:** Android 14 (API 34)

---

## 🚀 Para subir a Google Play

1. Genera una APK firmada (Release)
2. Crea una cuenta de desarrollador en Google Play Console (25€ una vez)
3. Sube la APK y completa la ficha de la app

¡Bendiciones! 🙏
