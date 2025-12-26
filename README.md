# BABA Wallet - Security Core Audit

### 🔒 Transparency & Security Verification

This repository contains the core security implementation for **BABA Wallet**. We are publishing this module to provide transparency regarding how user private keys and sensitive data are handled, encrypted, and stored on Android devices.

**Note:** This repository is for **security auditing purposes only**. The code is proprietary and is not open for redistribution or usage in other applications.

---

## 🛡️ Security Architecture

The `SecureStorage` module implements a defense-in-depth strategy using Android's native hardware-backed security features.

### Key Features:
1.  **Hardware-Backed Keystore:**
    * We utilize the Android Keystore System to generate cryptographic keys that never leave the device's hardware security module (TEE).
2.  **StrongBox Enforcement:**
    * On devices running Android P (API 28) and above, we explicitly request `StrongBox` backing, ensuring keys are stored in a separate secure hardware element.
3.  **AES-256 Encryption:**
    * Data is encrypted using `AES256_GCM` (Galois/Counter Mode) for authenticated encryption.
    * Keys are encrypted using `AES256_SIV`.
4.  **Non-Custodial Storage:**
    * Private keys are encrypted immediately upon creation. The application logic allows users to enable Biometric Authentication (Fingerprint/FaceID) as an access gate before decryption occurs.

## ⚠️ Compilation Note
This repository contains the `SecureStorage.kt` file in isolation. Dependencies such as the `Account` data model and `Base58` utilities are omitted to protect the full intellectual property of the application. This code is intended for reading and auditing, not for compiling or running independently.

## ⚖️ Legal & License
**Copyright (c) 2025 BABA Wallet / Rayan Pardaz Dadehaye Molaanaa. All Rights Reserved.**

* **View Only:** You are permitted to view and audit this code.
* **No Usage:** You are **not** permitted to copy, modify, distribute, or use this code in your own applications.
