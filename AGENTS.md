# AGENTS.md

## Project

Coin Tycoon 3D — LibGDX game. Gameplay/UI in Kotlin, procedural 3D graphics and
world helpers in Java. No external assets except the DejaVu fonts in `assets/`.

Modules: `core` (shared Kotlin+Java), `lwjgl3` (desktop), `android` (APK).

## Build

JDK 17+ and Android SDK (platform 34, build-tools 34.0.0). Set `sdk.dir` in
`local.properties`. Android SDK lives at `~/android-sdk` in this environment.

```bash
./gradlew core:test              # headless logic tests
./gradlew lwjgl3:run             # desktop
./gradlew android:assembleDebug  # APK -> android/build/outputs/apk/debug/
```

Headless runs need `LIBGL_ALWAYS_SOFTWARE=1` plus Xvfb. ALSA warnings about
audio devices are benign.

## Gotchas

- `ScreenUtils.clear(r, g, b, a)` passes `clearDepth = false`, so the depth
  buffer keeps stale values and every fragment fails the depth test. Always use
  the 5-arg overload with `clearDepth = true` in `render()`.
- Keep gameplay logic free of GL calls so it stays unit-testable headlessly:
  `CoinField` owns no rendering state, renderers only read from it.
- Android needs the natives jars unpacked into `libs/<abi>` (`copyAndroidNatives`
  task) for gdx and gdx-freetype, for all four ABIs.
