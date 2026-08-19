# AtmosSway

AtmosSway is a client-side compatibility mod for Minecraft 1.21.1 on NeoForge. It uses Project Atmosphere's wind to move SWAY-compatible foliage and angle precipitation while preserving SWAY's player and mob interactions.

## Features

- Combines ambient wind with SWAY's existing entity-contact forces.
- Supports vanilla and modded foliage registered through SWAY.
- Adds smooth nearby gust movement with stable variation between plants.
- Angles Project Atmosphere rain and snow, with Simple Clouds fallback support.

## Requirements

Install AtmosSway and these dependencies on the client:

- Minecraft `1.21.1`
- NeoForge `21.1.213` or newer for Minecraft 1.21.1
- [SWAY](https://modrinth.com/mod/sway) `2.4.3.x` (before `2.5`)
- [Project Atmosphere](https://modrinth.com/mod/project-atmosphere) `0.9.1.2.x` (before `0.10`)
- Simple Clouds `0.7.3.x`
- Project Atmosphere's required libraries and season mod

AtmosSway itself is client-only. Project Atmosphere and its dependencies may also be required on the server.

## Supported foliage

SWAY's registry and behavior pipelines determine compatibility. This includes SWAY's vanilla plants and modded plants explicitly supported through its API. Vines, sugar cane, double plants, and compatible multiblock foliage retain their shared SWAY behavior.

## Configuration

NeoForge creates `config/atmossway-client.toml` after the first launch.

| Option | Default | Description |
| --- | ---: | --- |
| `enabled` | `true` | Enables foliage wind and precipitation direction. |
| `debugLogging` | `false` | Writes aggregate wind diagnostics every 100 client ticks. |
| `windStrengthScale` | `0.10` | Converts wind speed into SWAY intensity. |
| `maxWindIntensity` | `2.0` | Caps wind-driven SWAY intensity. |
| `sampleIntervalTicks` | `1` | Controls how often Project Atmosphere wind is sampled. |
| `stabilizationWindowTicks` | `100` | Controls sustained-wind averaging time. |
| `renderChangeThreshold` | `0.05` | Minimum sustained wind change before distant foliage updates. |

Enable `debugLogging` when troubleshooting wind sampling, foliage application, or precipitation integration. Logging is rate-limited and never emitted per plant or rain streak.

## Building

Build with Java 21:

```powershell
.\gradlew.bat build
```
