# RootStealthLSP

🛡️ A powerful LSPosed module designed to stealthily bypass root and system modification detection for sensitive banking or security apps (e.g., com.ifast.gb).  
Built using the LSPosed API with full support for Zygisk-enabled environments.

## ✨ Features

- Blocks common root detection checks (e.g., file existence, mount flags).
- Hides suspicious paths like `/system/priv-app/AndroidAuto`, `/sbin/su`, etc.
- Works systemlessly with Magisk + Zygisk + LSPosed.

## 🧩 Requirements

- Android 8.0+  
- [LSPosed](https://github.com/LSPosed/LSPosed) (Zygisk recommended)  
- Magisk with Zygisk enabled  
- Banking or protected apps (e.g. `com.ifast.gb`)

## 🛠️ Build

```bash
git clone https://github.com/yourusername/RootStealthLSP.git
cd RootStealthLSP
./gradlew assembleDebug