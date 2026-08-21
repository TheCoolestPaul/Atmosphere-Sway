# AtmosSway

AtmosSway is a compatibility mod for Minecraft 1.21.1 on NeoForge. Its client rendering uses Project Atmosphere's wind to move SWAY-compatible foliage and angle precipitation while preserving SWAY's player and mob interactions. On dedicated multiplayer servers, a lightweight server component synchronizes the authoritative Project Atmosphere wind to each client.

## Features

- Combines ambient wind with SWAY's existing entity-contact forces.
- Supports vanilla and modded foliage registered through SWAY.
- Rebuilds the three nearest foliage sections together on an adaptive two-tick cadence, with immediate updates for gusts and proximity changes.
- Angles Project Atmosphere rain and snow, with Simple Clouds fallback support.
- Optionally directs Particle Rain's rain, snow, dust, and haze with the same wind.
- Synchronizes regional surface wind from dedicated servers every five server ticks.

## Requirements

Install AtmosSway and these dependencies on the client:

- Minecraft `1.21.1`
- NeoForge `21.1.213` or newer for Minecraft 1.21.1
- [SWAY](https://modrinth.com/mod/sway) `2.4.3.x` (before `2.5`)
- [Project Atmosphere](https://modrinth.com/mod/project-atmosphere) `0.9.1.2.x` (before `0.10`)
- Simple Clouds `0.7.3.x`
- Project Atmosphere's required libraries and season mod

Particle Rain is optional. AtmosSway supports its shared wind API in Particle Rain `4.0.0-beta.6` and newer. Older versions and installations without Particle Rain continue using their existing behavior.

For dedicated multiplayer, install the same AtmosSway version and Project Atmosphere on the server. SWAY and Simple Clouds remain client rendering requirements and do not need to be installed server-side for AtmosSway's wind synchronization.

The wind-sync channel is optional so mixed versions can still connect. If the server does not have a wind-sync-capable AtmosSway version, remote clients receive no ambient Project Atmosphere sway or wind-directed precipitation. Single-player continues to read Project Atmosphere wind directly from the integrated server.

## Supported foliage

SWAY's registry and behavior pipelines determine compatibility. This includes SWAY's vanilla plants and modded plants explicitly supported through its API. Vines, sugar cane, double plants, and compatible multiblock foliage retain their shared SWAY behavior.

When [Supplementaries](https://modrinth.com/mod/supplementaries) is installed, its flax crop and wild flax are registered with SWAY automatically. Supplementaries remains fully optional; AtmosSway does not load its classes or require Supplementaries or Moonlight to be present.

When [Immersive Weathering: Renewed](https://modrinth.com/mod/immersive-weather-renewed) is installed, AtmosSway also registers its dune grass, frosty grass, frosty fern, weeds, ivy, moss, and wall hanging roots. Wall hanging roots use SWAY's shared hanging-vine behavior so connected vertical segments move from their top anchor. Immersive Weathering and its Moonlight dependency remain fully optional; AtmosSway does not load either mod's classes or require either mod to be present.

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
| `renderChangeThreshold` | `0.05` | Minimum sustained wind change before the published base wind changes. |
| `particleRainAdapter` | `true` | Uses Project Atmosphere wind for all Particle Rain wind-reactive effects when supported. |
| `particleRainWindScale` | `0.06` | Converts metres per second into Particle Rain horizontal blocks-per-tick velocity. |

Enable `debugLogging` when troubleshooting wind sampling, foliage application, or precipitation integration. Logging is rate-limited and never emitted per plant or rain streak.

Diagnostic summaries identify the active `windSource` as `server_sync`, `integrated_direct`, or `unavailable`. They also report `syncAgeTicks`, the dimension/region match result, received/rejected packet counts, adaptive animation cadence skips, urgent passes, and the latest animation pass reason. On an updated dedicated server, `windSource=server_sync` confirms that multiplayer wind data is arriving.

Project Atmosphere can report genuinely calm conditions. A synchronized wind speed of zero intentionally produces no ambient foliage motion or precipitation tilt; SWAY's player and mob contact deformation continues to work normally.

When the Particle Rain adapter is active, Particle Rain retains its own per-effect wind multipliers, gravity, collisions, density, and rendering. AtmosSway only replaces the shared horizontal wind vector. Disabling either AtmosSway or `particleRainAdapter`, or starting before a valid Atmos wind sample is available, falls back to Particle Rain's native wind.

Nearby animation is intentionally capped at three sections. During steady wind, all three receive one synchronized pose every two client ticks; meaningful gust changes, player movement, and nearby selection changes refresh them immediately. Sections outside that set use the latest published base wind whenever Minecraft or an alternate chunk renderer naturally rebuilds them; gusts do not trigger staggered rebuilds across the distant loaded view.

## Building

Build with Java 21:

```powershell
.\gradlew.bat build
```
