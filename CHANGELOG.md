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
- Added `Vibrator` with patterns, amplitudes, predefined effects and haptic feedback
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