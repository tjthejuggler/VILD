# ADR: Star-driven dream sky (predictive color, shake shatter, tilt axis fix)

Date: 2026-08-22

## Context
The DreamBackground sky changed hue on a fixed 11s timer, decoupled from the starfield; the accelerometer X axis was sign-inverted (left-edge-down sent stars right); there was no shake interaction.

## Decision
1. **Tilt axes** (`AccelerometerEffect.kt`): sensor X is negated so tilt.x < 0 when the left edge dips — stars slide toward the downhill edge on both axes. TiltState gained a `shake` envelope computed as |‖a‖−g|/g with fast attack / slow decay (max(envelope*0.90, impulse*2.5)); pure tilt never changes gravity's magnitude, so the detector is tilt-invariant.
2. **Predictive color** (`DreamBackground.kt`): the hue timer was removed. Star speed is strictly proportional to lean (`baseSpeed * lean * 2.55 * depth`) — flat phone = still stars. Every star entering the screen (respawn behind the field, or edge-spawn during regather) is tinted `dreamPalette[nextHue]` and increments a charge; at 40 arrivals the sky crossfades (3s) to that color and a new prediction is drawn. Entry rate scales with tilt, so color-change pace scales with tilt.
3. **Shake shatter**: sustained shake (envelope > 0.55 for 0.3s, latched until envelope < 0.30) sets a FieldPhase state machine STREAM → BURST → REGATHER. BURST: all stars fly outward from screen center at 950 px/s and are nulled off-screen; skyLit=false fades sky, orbs and glow to Void (black) in 0.9s; charge resets. REGATHER: stars trickle back in through the uphill edge at ≤7/s scaled by lean, each tinted with the predicted color; 40 arrivals relight the sky. Flat phone after a shatter = permanently black until tilted.

## Consequences
- Color pacing is emergent (tilt-driven) instead of fixed; ~10s per change at full lean, frozen when flat.
- Tunables live as private consts (COLOR_CHARGE_NEEDED, SHAKE_TRIGGER/HOLD/REARM, BURST_SPEED, REGATHER_RATE, SLIDE_GAIN).
- TiltState consumers (MainActivity, SettingsScreen, StatsScreen) unaffected — new field has a default.