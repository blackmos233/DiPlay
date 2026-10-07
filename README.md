# DiPlay

**CarPlay for compatible BYD Android head units.** Wired and wireless, with the familiar DiAuto interface. Independent app: `com.shihab.diplay`.

> **BYD support scope:** These projects focus on BYD cars. They may work on other brands, but other brands are unsupported and there are no plans to add support or fix brand-specific incompatibilities.

[Download & website](https://shihabal3amri.github.io/DiPlay/) · [Release](https://github.com/shihabal3amri/DiPlay/releases/tag/v0.2.13) · [Report a problem](https://github.com/shihabal3amri/DiPlay/issues/new/choose)

![DiPlay home](site/assets/home.png)

## 0.2.13 — public preview

Install on the **car**, not the iPhone. No jailbreak, dongle, Mac, account or authentication server is required for use. Core CarPlay does not require ADB; optional dashboard, battery, wheel-speed and parked-video features do. Your head unit must permit APK installation. The APK supports Android 9+ (API 28); wireless supports Wi-Fi Direct, the car’s existing hotspot or Existing Wi-Fi / Same LAN. Android 9 Wi-Fi Direct uses a firmware-dependent legacy path with generated group credentials and unverified requested frequency; see [Android 9 Wi-Fi Direct](docs/ANDROID9_WIFI_DIRECT.md). Android 10+ verifies its negotiated group frequency.

- Wired USB and wireless CarPlay with local authentication.
- BYD HUD navigation with arrows, distance and street names on verified firmware.
- Car hotspot support, improved audio buffering and saved receive diagnostics.
- Automatic address discovery, fixed-channel Wi-Fi fallbacks and successful-configuration memory.
- Icon/text size, resolution and frame rate; applying a display change reconnects CarPlay.
- Local diagnostic export. Reports are sent only if you choose to share them.
- Separate installation alongside DiAuto. Run one projection app at a time.

This is **not an Apple-certified product**. The APK bundles an experimental accessory identity recovered from public Carlinkit firmware, not a newly provisioned MFi identity for DiPlay. A bundled private key is extractable. Acceptance after future iOS updates, reliability across head units and suitability of that identity for general distribution are unresolved. This release invites community testing; it is not a guarantee of universal compatibility.

Earlier releases were tested on the development DiLink5.1 car: live windshield guidance and street names work, Car hotspot now starts CarPlay, and Wi-Fi Direct performance is substantially improved. Occasional audio cutouts remain and are deferred to a later update. The floating-map test build was installed on the development DiLink 5.1 car; feedback led to the pinch corrections in 0.2.9. Earlier wheel-speed and video contributions were tested on a BYD Tang with DiLink 5.0 and an iPhone 15 Pro on iOS 27; wheel-speed dead reckoning in tunnels remains unverified. Broader head-unit and iOS compatibility is not guaranteed. The HUD firmware scope and cleanup limits are documented in [BYD navigation](docs/BYD_NAVIGATION.md).

## What’s new in 0.2.13

- Android 9 Wi-Fi Direct, IPv4-first hotspot endpoints, safer Auto channel ordering and wireless startup without unused NSD/USB services.
- Guarded rendered-video handoff fallback, safe wireless-to-USB switching and checked, cancellable permission setup.
- Narrow USBMUX trailer recovery that preserves valid payload replies.
- Optional live dock/split-screen areas and square-canvas rotation, plus the selected-decoder capability check and default-off experimental side panel.
- DiLink 4 casting/calibration and live cluster picture controls; checked DiLink 3 projection entry with compensation.
- Recent turn-card retention across wireless replacement, a finer dashboard map choice, battery-protocol fallback and eligible wheel-service recovery.
- The observed Siri microphone timestamp correction and TCP_NODELAY touch events, with device-specific performance limits.
- **Off by default:** independent experimental DiLink 3 call keys/dashboard calls and AAC-LC buffered music. Read their firmware/audio/restoration limits before opting in. Eligible hotspot join repair is a separately confirmed Check/Apply/Restore action.
- Clearer settings, saved-menu behavior, car-button customization and day/night-aware waiting screens.
- Traditional Chinese (Taiwan), bringing both the app and release website to seven languages.

See [0.2.13 release notes](docs/RELEASE-NOTES-0.2.13.md) and [validation](docs/VALIDATION.md) for all reviewed contributions, hardware evidence and remaining physical tests. Higher resolution and large square canvases cost more decoder/GPU work. General stutter, calls/Siri, decoder, old-iOS startup and model-specific reports remain under investigation. [0.2.12 notes](docs/RELEASE-NOTES-0.2.12.md) remain available as historical guidance.

If a problem remains, reproduce it on **0.2.13**, then use **Settings → Diagnostics → Save diagnostic report**. Android 10+ normally saves to **Downloads/DiPlay**; Android 9 uses the document picker. If unavailable, use **View report** or **Share** from the confirmation, which identifies external/private fallback storage. Review the `.txt` and add it to a matching [existing issue](https://github.com/shihabal3amri/DiPlay/issues), or [create one](https://github.com/shihabal3amri/DiPlay/issues/new/choose). Include vehicle/head-unit model, exact firmware and Android/DiLink, phone/iOS, connection backend, relevant settings, steps and failure time. Reports are shared only when you choose; never post your hotspot password.

## Extended Features (DiPlay Enhanced)

In addition to upstream functionality, this fork introduces key enhancements for customized car launchers, performance optimization, and seamless integration:

- **Root Fast Direct Bluetooth Connection (Skip SDP Query)**:
  - Bypasses the slow Bluetooth Service Discovery Protocol (SDP) step and directly connects to the iPhone's fixed iAP2 RFCOMM channel.
  - Coupled with system I/O tuning, wireless connection setup time is reduced by 4–6 seconds.
  - Can be toggled in Connection Settings ("跳过SDP连接" / Skip SDP direct connect).

- **External Invocation API & Custom Launcher Integration**:
  - Full external control chain supporting third-party Launchers and automation tools.
  - **Broadcast API**: Non-intrusive control without stealing focus:
    - `com.shihab.diplay.action.CONNECT` (supports `silent=true`, `fast_rfcomm=true`)
    - `com.shihab.diplay.action.DISCONNECT`
    - `com.shihab.diplay.action.OPEN_CARPLAY`
    - `com.shihab.diplay.action.GET_STATUS`
  - **Status Broadcast Distribution**: `com.shihab.diplay.action.STATUS_CHANGED` broadcasts real-time status (`IDLE`, `CONNECTING`, `CONNECTED`, `DISCONNECTED`) and granular stage descriptions.
  - **Cold-Start Trampoline Activities**: Zero-animation translucent Activities (`ConnectActivity`, `DisconnectActivity`, `OpenCarPlayActivity`) that can wake up DiPlay even after the process was killed or from system cold boot.

- **Silent Background Boot & Connection**:
  - Support for `silent=true` mode: DiPlay completes Bluetooth and Wi-Fi negotiation entirely in the background without bringing the full-screen CarPlay UI forward.
  - Settings option for "开机后台静默连接" (Auto silent connect on boot) so CarPlay is already connected when you step into the car, allowing instantaneous full-screen access upon clicking your launcher card.

- **Granular Live Progress on Connection Screen**:
  - Replaces generic "Preparing CarPlay..." with real-time handshake step feedback:
    - Bluetooth RFCOMM fast direct link / SDP query
    - Vehicle Wi-Fi hotspot ready & awaiting phone
    - Credential exchange & Wi-Fi association
    - Establishing AirPlay session & rendering video stream.

- **Companion Test App (`com.diplay.tester`)**:
  - Standalone `:tester` module providing an interactive, landscape-optimized dual-column dashboard for testing broadcasts, cold wakeups, and monitoring real-time event logs.

## Documentation

[Existing Wi-Fi / Same LAN](docs/EXISTING_WIFI.md) keeps the iPhone and head unit
on an external router. See the guide for setup, build requirements and the
BYD DiLink 4.0 / Android 10 clean-install validation result.

- [External Control API & Launcher Integration](docs/EXTERNAL_CONTROL_API.md)
- [Install and connect](docs/INSTALL.md)
- [Compatibility and troubleshooting](docs/COMPATIBILITY.md)
- [Smooth wireless CarPlay](docs/SMOOTH_WIRELESS.md)
- [Privacy and diagnostic reports](docs/PRIVACY.md)
- [Build from source](docs/BUILD.md)
- [Validation](docs/VALIDATION.md)
- [Release notes](CHANGELOG.md)
- [Credits and licenses](docs/THIRD_PARTY_NOTICES.md)

The app and release website are available in English, Arabic, Russian, Ukrainian, Spanish, Simplified Chinese and Traditional Chinese (Taiwan). Traditional Chinese uses Taiwan wording; the app also recognizes Hong Kong/Macao and explicit Hant selections without claiming separate regional translations. Choose the app language in Settings; on Android 13+, it stays synchronized with Android’s per-app language setting.

## Source and credits

Based on [xcertplay](https://github.com/shilapi/xcertplay), GPL-3.0. The home/settings UI and website adapt [DiAuto](https://github.com/shihabal3amri/DiAuto), AGPL-3.0; that license is included in `docs/licenses`. Preserve those notices when distributing modifications. CarPlay and its icon belong to Apple Inc.; no Apple or BYD affiliation or endorsement is implied.

This repository starts with a clean public source snapshot. Local research, tester reports and release-signing secrets are excluded. The complete source corresponding to the APK is provided with every release; experimental runtime identity assets are described separately in the build instructions and notices.

## Local release packaging

The release APK intentionally contains the experimental accessory identity. The Git repository and source archive exclude all accessory and Android signing keys; tests generate synthetic identities at runtime. Source/CI builds omit runtime identity assets by default. Local release builds explicitly select an external asset directory. Publishing the APK makes its bundled identity extractable; building locally does not preserve that identity's confidentiality.
