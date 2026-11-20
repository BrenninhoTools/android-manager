Changelog
=========

1.2.0 (??/??/2026)
------------------

- Added `Callback` with typed events for activity results, permission results, lifecycle, back button, new intents and memory trimming
- `CallbackManager` is now registered as a Lime extension, so lifecycle events actually fire
- `Lifecycle` now runs on top of `Callback` and gained `addOnResume` and `addOnDestroy`
- `PermissionUtils` gained result callbacks, granted/requested/denied lists, rationale check, all files access, overlay, write settings and install packages requests
- Added `Permission` constants
- Added `Device` (root, emulator, tablet, TV, Chromebook, DeX, Dolby Atmos, dark mode, thermal status, memory, storage and display info)
- Added `Window` (secure mode, keep screen on, immersive, cutout, orientation, brightness, refresh rate, sustained performance, bar colors)
- Added `Application` (app info, launch packages, open URLs, share, store page, custom intents, restart and exit)
- Added `Notification` with channels, priorities, progress and permission request
- Added `AudioManager` with volume control, audio focus events and output device info
- `AudioManager` gained volume, device, headphones, becoming noisy, ringer and mode events, focus helpers (`onShouldPause`, `onShouldResume`, `onDuck`, `autoHandle`), detailed devices, latency and low latency info, spatial audio, mute, volume buttons stream and system sound effects
- Added `Screenshot` with file, gallery, base64 and `Image` output, crop, scale, formats, delay, shutter sound, sharing and Android 14 screenshot detection
- Added `Storage` with volumes and SD card info, space, writability checks, game folder picker, `.nomedia`, file listing, public folders through MediaStore and async copy, move, delete, zip, unzip and size
- Added `Share` with a built in read only content provider, multiple files, text, target apps and chosen app callback
- `Screenshot.share` and `Screenshot.shareTo` share through the content provider without saving to the gallery
- Removed the `androidx.core` dependency line that had no effect
- Added `Vibrator` with patterns, amplitudes, predefined effects and haptic feedback
- `Vibrator` gained composition primitives, primitive and effect support checks, primitive durations, vibration usage attributes, resonant frequency, Q factor and vibrator count
- Added `Input` with low latency touch dispatch, touch slop, tap, double tap and long press timeouts, fling velocities, density, max touch points, input source checks and the system touch feedback setting
- Added `PlayGames` (Google Play Games Services v2) with sign in, server side access, player info and avatar, achievements, leaderboards, events and saved games, enabled with `PLAY_GAMES_APP_ID`
- `PlayGames` connection modes (`auto`, `silent`, `manual`) with state events, automatic prompt limits, remembered declines and silent re-check on resume
- `Battery` rewritten: typed events (level, charging, power connected, low, power save, doze, thermal, overheat), level alerts, current, watts, capacity, health percent, cycle count, time to full and empty, thermal headroom and battery settings shortcuts
- `Window` gained resize, orientation, focus, insets and keyboard events, safe area, separate system bar control, light bars, edge to edge, show when locked, display modes, high refresh rate, surface frame rate, color mode, keyboard control and screen capture
- Added `Clipboard`
- Added `Network` with connection type and change listener
- Added `FileManager` using the Storage Access Framework (pick files and folders, create documents, copy to and from URIs, list and copy folders)
- Added `Settings` to read system, secure and global values and open settings screens
- `Interface` gained styled and native toasts, three button dialogs, input, list, single choice, multi choice and progress dialogs
- Dialog and JNI callbacks now run on the Haxe main thread (Lime 8+)
- `JNICache` gained member methods, a generic `call` helper and no longer prints traces
- Fixed `Context.getExternalFilesDir` calling a Java method that did not exist

1.1.0 (??/??/2026)
------------------

- Functions for logcat have been added
- Added function to retrieve the "Android/data" directory from the app
- Improvements to PermissionUtils.hx
- Improvements to Context.hx
- Added Lifecycle.hx

1.0.1 (06/17/2026)
------------------

- Fixed bug where permissions were not being requested
- Improvements and optimization in Java
- Readme now contains information