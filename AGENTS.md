# AGENTS.md

## Project

Coin Tycoon 3D — LibGDX game. Gameplay/UI in Kotlin, procedural 3D graphics and
world helpers in Java. No external assets except the DejaVu fonts in `assets/`
and the sound effects, which are synthesised in code (see `com.tycoon.audio`).

Modules: `core` (shared Kotlin+Java), `lwjgl3` (desktop), `android` (APK).

Screens live in `core/.../screen/`: a main menu (play / block world / options /
shop / quit), the options screen and the shop are `MenuScreen` subclasses; the
gameplay screens are `GameScreen` (coin arena) and `VoxelScreen` (block world).
`TycoonGame` owns the shared `Skin`, `Settings` and `TycoonState` and swaps
screens, disposing the outgoing one (never the skin).

Both gameplay modes feed the same `TycoonState`, so coins earned by mining in
the block world can be spent on the arena upgrades and vice versa.

The block world is split along a Kotlin/Java seam that keeps it testable:
`com.tycoon.voxel` (pure Java, no GL) owns `BlockType`, `VoxelWorld`,
`TerrainGenerator`, `VillageBuilder`, `StallFarm`, `VoxelRaycast`,
`BlockGeometry`, `VoxelPlayer` and `VoxelInventory`; `gfx/VoxelRenderer.kt` and
`screen/VoxelScreen.kt` are the only places that touch OpenGL. `BlockGeometry`
culls hidden faces and returns plain vertex/index arrays, so the meshing can be
asserted in a headless test.

The surface is a village: `VillageBuilder` levels a market square, rings it with
market stalls and drops cottages on flat ground. Each `BlockType.STALL` block is
a farm — `StallFarm` indexes them and pays coins on a timer, feeding the same
`TycoonState` the arena uses.

## Build

JDK 17+ and Android SDK (platform 34, build-tools 34.0.0). Set `sdk.dir` in
`local.properties`. Android SDK lives at `~/android-sdk` in this environment.

The sandbox can be reset between sessions, wiping the JDK, Gradle caches and
the Android SDK. To restore: `sudo apt-get install -y openjdk-21-jdk-headless`,
then unpack the Google cmdline-tools into `~/android-sdk/cmdline-tools/latest`
and install `platform-tools`, `platforms;android-34`, `build-tools;34.0.0`.

```bash
./gradlew core:test              # headless logic tests
./gradlew lwjgl3:run             # desktop
./gradlew android:stageApk       # APK -> android/build/dist/CoinTycoon3D-debug.apk
```

Use `android:stageApk`, not `assembleDebug`: AGP names the raw output
`android-debug.apk`, and the release download link needs a stable file name.

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
- `PerspectiveCamera` builds its frustum through JNI, so any test that
  constructs one needs `natives-desktop` on the test runtime classpath and a
  call to `GdxNativesLoader.load()` before the camera is created.
- `Skin.add(name, obj)` keys entries by the object's concrete class, so a
  `TextureRegionDrawable` is invisible to `getDrawable`. Register drawables with
  the explicit `Drawable.class` argument.
- `Slider` and `ScrollPane` look their style up under `default-horizontal` /
  `default-vertical`, not `default`, so `Skin.get("default", SliderStyle)` fails
  at runtime.
- Inside `Table().apply { ... }` a bare `stage` resolves to `Actor.getStage()`,
  which is null until the actor is added to a stage — not the screen's `stage`.
  Build the table in a function instead.
- Change screens from button callbacks with `TycoonGame.swapLater`: listeners run
  inside the stage's `act`, so swapping directly would dispose the tree mid-walk.
- `ClickListener` is an abstract class, so it cannot be a SAM lambda; use the
  `onClick { }` extension in `ui/Clicks.kt`.
- Audio is procedural: `SoundSynth` (Java, no Gdx) renders WAV bytes and
  `SoundBank` writes them to `Gdx.files.local("sfx/...")` before loading. The
  Android backend's `SoundPool.load` needs a real file path, so the bytes cannot
  be played straight from memory; `sfx/` is generated at runtime and gitignored.
- `SoundBank` must be constructed after `Gdx.app` exists (in `create()`), not in
  a field initialiser, because it touches `Gdx.files` and `Gdx.audio`.
- The block world is generated from `TycoonState.voxelSeed` and the player's
  edits are replayed on top, so the save file stores a seed plus an edit list
  instead of 200k block ids. `VoxelScreen.saveVoxelState` flattens edits to
  `[x, y, z, blockId]` quads; keep that layout in sync with `replayEdits`.
- `BlockGeometry` meshes a chunk into one `Mesh`, so a chunk must stay under
  65535 vertices for short indices. `CHUNK_SIZE = 16` keeps the worst case at
  ~33k vertices; a larger chunk would need 32-bit indices.
- Editing a block only dirties its own chunk, except on a chunk border, where
  the neighbour's visible faces change too — `VoxelScreen.markDirtyAround`
  handles both.
- `Mesh` built from raw arrays goes through `ModelBuilder.part(...)`, not
  `ModelBuilder.createBox`, so `VoxelRenderer` must dispose the resulting
  `Model` (which disposes the mesh) when a chunk is rebuilt.
