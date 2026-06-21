<p align="center">
  <img src="preview/re.png" width="100" height="100" alt="RePairip logo"/>
</p>

<h1 align="center">RePairip</h1>

<p align="center">
  Universal Pairip protection remover. Works on all apps using any Pairip SDK version.
</p>

---

## ⚡ Termux — No building needed

```bash
# 1. One-time setup
termux-setup-storage
pkg install wget openjdk-17 -y

# 2. Download the pre-built JAR
wget https://github.com/Matrix1999/RePairip/releases/download/v1.0/RePairip.jar

# 3. Run it
java -jar RePairip.jar -i /sdcard/Download/yourapp.apks
```

Patched APK → `/sdcard/RePairip/`
Full log → `/sdcard/RePairip/logs/`

---

## 🔧 Termux — Clone & Build

```bash
termux-setup-storage
pkg install git openjdk-17 -y

git clone https://github.com/Matrix1999/RePairip.git
cd RePairip
chmod +x gradlew
./gradlew :Matrix:shadowJar

java -jar Matrix/build/libs/RePairip.jar -i /sdcard/Download/yourapp.apks
```

---

## Options

| Flag | Description |
|------|-------------|
| `-i <file>` | Input `.apk` or `.apks` file (required) |
| `-t <file>` | Translation JSON file (optional) |
| `--no-log` | Disable sdcard log file (ON by default) |

---

## What it patches

- All `com.pairip.*` license/signature check classes (universal, not just 3 hardcoded)
- `verifySignatureMatches`, `verifyIntegrity`, `checkLicense`, `validateSignature`, `checkSignature`, `isValid`, `isLicensed`, `verify`, `check` and more

---

## License

Apache License 2.0 — Copyright (C) 2026
