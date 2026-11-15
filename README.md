# Epic Donation Animation

Client-side Fabric mod for Minecraft 1.21.11.

> Build status: the project source is provided; the first GitHub Actions run will verify compilation. No tested JAR is claimed until that run succeeds.

## Build locally

- Java 21
- Gradle 9.x
- Run `gradle build`

The JAR will be written to `build/libs/`.

## Features

- Chat-based donation message detection (server wording may need adaptation)
- Six HUD animation tiers
- Settings screen opened with `O`
- JSON configuration for thresholds, duration, and HUD anchor

See the source and configuration for implementation details.
