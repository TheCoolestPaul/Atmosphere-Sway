# AtmosSway

AtmosSway is a small client-side compatibility mod for Minecraft 1.21.1 on NeoForge. It initializes SWAY's own foliage registry and adds Project Atmosphere's surface wind to SWAY's existing player and mob interaction forces.

Wind and contact forces are combined as vectors: motion in the same direction becomes stronger, angled forces produce a combined direction, and opposing forces can partially cancel.

To keep nearby foliage from forming one perfectly uniform lean, AtmosSway gives each plant a
deterministic spatial variation of up to 8 degrees and 10 percent ambient-wind strength. The value
comes from the plant's position, so it remains stable across rebuilds and adds no recurring render
work. SWAY multiblock structures share their pipeline-defined anchor, keeping vines, double plants,
and sugar cane visually continuous. Contact forces are added after this variation and are not
randomized.

Foliage near the player also receives a subtle five-second animated wave. It adds up to `0.055`
SWAY force across the wind and `0.025` along the wind, providing visible movement during light wind
while smoothly fading to no animation at calm. Animation is limited to swayable sections within two
sections horizontally and vertically. The six nearest qualifying sections animate continuously at a
budget of at most three exact-section rebuilds per client tick, producing a new pose every two ticks
under a full workload. Smaller sets update every tick. Distant foliage keeps its stable wind pose.
This prioritizes smoother nearby movement without increasing the rebuild ceiling. SWAY deforms baked
terrain geometry, so poses cannot be interpolated independently on every rendered frame without a
separate shader-driven rendering path. AtmosSway also limits SWAY's entity/contact scan to one run
per client game tick and compensates its internal delta time, avoiding redundant high-FPS scans
without changing contact decay speed.

Nearby foliage also reacts to Project Atmosphere gusts sooner than the stable loaded-view pose. A
short vector filter follows sustained raw-wind changes over roughly three quarters of a second, then
blends half of that change into the nearby wind with a maximum adjustment of `0.10` SWAY force. The
six animated sections receive this wind through their existing rebuilds, adding no work. Other
swayable sections within the two-section radius update nearest-first only after a meaningful gust
change, at a secondary budget of two exact sections per tick and no more than one propagation start
per ten ticks. The combined animation and gust ceiling is therefore five invalidations per tick.
Distant foliage continues using only the stable five-second wind latch.

When that five-second latch accepts a new baseline, the six animated sections retain their previous
pose and blend out the commit-induced vector difference over 20 ticks. Their existing two-tick pose
cadence supplies roughly ten intermediate poses without adding section rebuilds or delaying new gusts.

Project Atmosphere's native rain and snow columns follow its raw surface-wind direction through a
frame-interpolated, visual-safe top offset. The offset uses
`2 * speedMps / (speedMps + 6)` blocks, so calm precipitation stays vertical and extreme wind
approaches a two-block displacement without opening large gaps in Atmosphere's limited render area.
When Project Atmosphere does not own precipitation, the Simple Clouds fallback instead uses a
physical `atan(windSpeed / 9 m/s)` tilt capped at 85 degrees. Both paths smooth the wind vector over
roughly half a second. Disabling AtmosSway restores the renderer's original precipitation angle.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.213 or newer in the 1.21.1 line
- SWAY 2.4.3.x (before 2.5)
- Project Atmosphere 0.9.1.2.x (before 0.10)
- Simple Clouds 0.7.3.x
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
debugLogging = false
windStrengthScale = 0.10
maxWindIntensity = 2.0
sampleIntervalTicks = 1
stabilizationWindowTicks = 100
renderChangeThreshold = 0.05
```

AtmosSway averages Project Atmosphere's raw surface-wind vector over
`stabilizationWindowTicks`, then holds that exact deformation until the averaged vector differs
from the rendered wind by at least `renderChangeThreshold`. The defaults respond to sustained
changes in about five seconds while filtering short gust spikes that would otherwise pulse baked
foliage geometry and repeatedly rebuild render sections.

The five-second hold controls the distant loaded view. It does not prevent the fixed, budgeted
near-field gust response described above; no additional gust configuration is required.

`sampleIntervalTicks` controls how often Project Atmosphere is queried. Cached samples are
time-weighted across the stabilization window, so increasing the sampling interval does not shorten
the five-second hold. New chunks use the same committed wind as existing chunks.

Set `debugLogging = true` to emit one aggregate diagnostic line every 100 client ticks. The summary reports the raw Project Atmosphere sample, completed window average, committed render wind, fast-smoothed wind, gust adjustment, propagation target, displayed animation wind, commit-transition correction/progress, gust propagation queue and invalidations, animation pose, observed pose-step ticks and pass activity, the active-section cap and rebuild budget, precipitation renderer/speed/heading/tilt/native offset, active crosswind and along-wind animation envelopes, executed and suppressed SWAY scans, section-build snapshots, SWAY model-hook activity, contact combinations, and exact section invalidations. AtmosSway never logs once per block or precipitation streak.

An unexpected render-view type or repeated inability to resolve the client level is always logged once as a warning because it indicates that ambient wind cannot reach those models.

Ambient wind covers SWAY-compatible blocks in loaded render sections throughout Minecraft's effective view distance. Sections are rebuilt only after a meaningful wind change, when AtmosSway is toggled, or as new terrain is loaded.

SWAY's own `influenceRadius` continues to control player and mob contact only. Contact deformation is vector-added to ambient wind, then returns to wind-only deformation as the interaction decays.

## Building

Use Java 21:

```powershell
.\gradlew.bat build
```

The finished JAR is written to `build/libs/`.
