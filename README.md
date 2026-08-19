# AtmosSway

AtmosSway is a small client-side compatibility mod for Minecraft 1.21.1 on NeoForge. It initializes SWAY's own foliage registry and adds Project Atmosphere's surface wind to SWAY's existing player and mob interaction forces.

Wind and contact forces are combined as vectors: motion in the same direction becomes stronger, angled forces produce a combined direction, and opposing forces can partially cancel.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.213 or newer in the 1.21.1 line
- SWAY 2.4.3 or newer
- Project Atmosphere 0.9.1.2 or newer
- Project Atmosphere's required runtime stack, including Gabou's Libs, Simple Clouds, and a supported season system such as Serene Seasons

AtmosSway is only needed on the client. Project Atmosphere may still require installation on both the client and server.

## Supported blocks

SWAY's registry and behavior pipelines are the source of truth. This includes SWAY's vanilla plant set—grasses, ferns, flowers, crops, fungi, saplings, aquatic plants, vines, sugar cane, and similar plants—and modded blocks explicitly registered through the SWAY API.

AtmosSway recognizes direct block registrations, custom pipelines, and matching global behaviors. A mod must register its SWAY integration before model baking; registrations made later require a resource reload. Solid tree leaf blocks are excluded unless a mod explicitly gives them a SWAY deformation pipeline.

Vines and sugar cane use SWAY's specialized continuous multiblock deformation behavior.

## Configuration

NeoForge creates `config/atmossway-client.toml` after the first launch:

```toml
[wind]
enabled = true
windStrengthScale = 0.08
maxWindIntensity = 1.5
sampleIntervalTicks = 1
```

Ambient wind covers SWAY-compatible blocks in loaded render sections throughout Minecraft's effective view distance. Sections are rebuilt only after a meaningful wind change, when AtmosSway is toggled, or as new terrain is loaded.

SWAY's own `influenceRadius` continues to control player and mob contact only. Contact deformation is vector-added to ambient wind, then returns to wind-only deformation as the interaction decays.

## Building

Use Java 21:

```powershell
.\gradlew.bat build
```

The finished JAR is written to `build/libs/`.
