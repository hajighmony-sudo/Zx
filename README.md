# ZX VPN

**Every way through, in one app.**

**English** · [فارسی](README.fa.md)

ZX VPN is a multi-engine VPN and proxy client. It brings several networking engines and connection types together behind one interface.

> Project status: Engine availability differs by platform, build variant and release. Check release notes before relying on a specific engine.

## Contents

* [Highlights](#highlights)
* [Engines and protocol support](#engines-and-protocol-support)
* [Aether integration](#aether-integration)
* [GHOST MOD](#ghost-mod)
* [Themes](#themes)
* [Configuration import and management](#configuration-import-and-management)
* [Connection tools](#connection-tools)
* [Platform support](#platform-support)
* [Download and installation](#download-and-installation)
* [Build from source](#build-from-source)
* [Privacy and security notes](#privacy-and-security-notes)
* [License](#license)

## Highlights

* Multi-engine architecture, configuration management, subscriptions, server testing, connection chaining, per-app routing, advanced routing, DNS utilities, diagnostics, vault sharing, modern Material 3 UI, desktop client.

## Engines and protocol support

| Engine | Use cases |
| --- | --- |
| **Xray** | VLESS (Reality, Vision, XHTTP), VMess, Trojan, Shadowsocks, Hysteria2, WireGuard / AmneziaWG |
| **sing-box** | TUIC, Naive, AnyTLS, ShadowTLS, OpenVPN, sing-box JSON |
| **Psiphon** | Psiphon network service |
| **Tor** | Tor + bridges (obfs4, Snowflake, Conjure) |
| **DNS tunnels** | DNSTT, VayDNS, MasterDNS |
| **OpenConnect / IKEv2 / SSH** | Platform VPN and tunnel modes |
| **Aether (Android)** | Cluvex + QW-patched Aether integration |

## Aether integration

Smart config and connection ladder for filtered networks. See in-app descriptions and release notes.

## GHOST MOD

GHOST MOD is ZX's advanced anti-censorship system: multi-layer DPI resistance with a live status dashboard.

- **Layers:** Detection & Status → Soft Camouflage (Reality + uTLS + XHTTP + Fragment) → Hard Desync → Statistical Shaping → Adaptive Ladder → Fallback (Hysteria2 / AmneziaWG / DNS / Psiphon / Tor)
- **Status:** DPI level (None/Low/Medium/High/Extreme), techniques, Iran-time last check, visibility (Invisible / Under pressure / Adapting)
- **Profiles:** `iran_mobile`, `iran_home`, `russia`, `china`, `auto`, `custom`
- **Anti-Porn** (unchanged): family-safe DNS + domain/keyword block + DoH endpoints (up to 500 custom domains)

Implementation files live under `shared/.../core/ghost/` (`GhostStatusManager`, `GhostDpiDetector`, `GhostLayerProfiles`, `GhostEngine`, …).  
Full technical specification: [docs/GHOST_MODE_ADVANCED.md](docs/GHOST_MODE_ADVANCED.md)

## Themes

Bento, Pixel, Old, ZX (cyberpunk), ZX PS (glass). Same launcher icon and app label in every theme.

## Configuration import and management

VLESS/VMess/Trojan/Shadowsocks links, Xray/sing-box JSON, subscriptions, OpenVPN, Amnezia, QR, ZX Vault `.zsx`, local share intents.

## Connection tools

Server testing, connection chaining, DNS utilities, per-app routing, vault sharing.

## Platform support

Android (ABI-specific APKs), Linux, Windows, macOS. Engine coverage depends on the packaged build.

## Download and installation

Official builds: [GitHub Releases](https://github.com/hajighmony-sudo/Zx/releases).

## Build from source

Clone with submodules, JDK 21, Android SDK/NDK, Rust targets for Android, then use the included Gradle wrapper and `tools/` scripts. See repository scripts and CI workflows.

## Privacy and security notes

Treat exported vault packages as sensitive. Install only from trusted release assets and verify checksums when provided.

## License

See [LICENSE](LICENSE), [NOTICE](NOTICE), and [COPYRIGHT](COPYRIGHT).
