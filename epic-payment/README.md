# Epic Payment Animations (Fabric 1.21.11)

Client-side Fabric mod that watches incoming server game/chat messages and displays a tiered cinematic HUD animation for detected payments.

## Build

- Java 21
- Minecraft 1.21.11
- Fabric Loader 0.18.1+
- Fabric API 0.141.1+1.21.11
- Fabric Loom 1.11

Run `gradle build` with Gradle 8.14.3+ and network access. The distributable jar is created in `build/libs/`.

## Controls

Press **O** (rebindable in Options → Controls) to open settings. The settings screen toggles animations, sound and particles, adjusts duration, and previews each tier.

## Configuration

`config/epicpayment.json` is created on first launch. Thresholds, tier names/colors, minimum amount, duration, and overlay position can be edited there. Optional `customPatterns` are Java regular expressions; capture group 1 must contain the amount and group 2 may contain the sender name. Example: `"Du hast ([0-9.,]+\\s*[kmbt]?) von ([A-Za-z0-9_]+) erhalten"`. Tier thresholds are sorted on load; the highest threshold not exceeding the payment is selected.

## Detection notes

The parser intentionally recognizes positive/inbound patterns only: `Du hast ... erhalten/bekommen`, `<player> hat dir ... gegeben/gesendet/überwiesen`, and `+amount` with an explicit currency/coin marker. Server messages are not standardized, so unusual server wording may need a custom parser pattern. The mod does not change server data or send commands.
