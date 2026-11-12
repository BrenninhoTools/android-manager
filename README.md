## Android Manager

![RepoSize](https://img.shields.io/github/repo-size/BrenninhoTools/android-manager
) ![Discord](https://img.shields.io/discord/1518622560214978620?logo=discord&color=blue)
 ![Licença MIT](https://img.shields.io/badge/license-MIT-green)

![Android-Manager](https://raw.githubusercontent.com/BrenninhoTools/android-manager/refs/heads/main/docs/logo.png)

A [Haxe](https://haxe.org/)/[Lime](https://lime.openfl.org/) extension for android devices

## Installation

1. You can download the latest version of Android-Manager using haxelib install
   ```bash
   haxelib install android-manager
   ```

   Or you can download it from Github and use the current changes (which may be EXPERIMENTAL)
   ```bash
   haxelib git android-manager https://github.com/BrenninhoTools/android-manager
   ```

2. Then, you'll need to add this code to your project's project.xml file
   ```xml
   <haxelib name="android-manager" if="android"/>
   ```

Great, now you can use the library!

## Modules

| Module | What it does |
| --- | --- |
| `androidmanager.callback.Callback` | Typed events: `onActivityResult`, `onRequestPermissionsResult`, `onPause`, `onResume`, `onBackPressed`, `onTrimMemory`, `onNewIntent`... |
| `androidmanager.tools.PermissionUtils` | Runtime permissions with result callbacks, all files access, overlays, write settings, install packages |
| `androidmanager.tools.Lifecycle` | `addOnExit`, `addOnResume`, `addOnDestroy` |
| `androidmanager.os.Device` | Root/emulator/tablet/TV/Chromebook/DeX detection, memory, storage, display and thermal info |
| `androidmanager.view.Window` | Secure mode, keep screen on, immersive, cutout, orientation, brightness, refresh rate |
| `androidmanager.app.Application` | App info, open URLs, share, launch packages, custom intents, restart, exit |
| `androidmanager.app.Notification` | Channels, notifications with progress, permission request |
| `androidmanager.media.AudioManager` | Stream volume, audio focus events, headphones and output devices |
| `androidmanager.os.Vibrator` | Vibration patterns, amplitudes, predefined effects, haptic feedback |
| `androidmanager.content.FileManager` | File/folder pickers (SAF), copy between URIs and paths, list and import folders |
| `androidmanager.content.Interface` | Toasts, alerts, confirm, input, list, single/multi choice and progress dialogs |
| `androidmanager.content.Clipboard` | Read and write the clipboard |
| `androidmanager.net.Network` | Connection status, type, metered and change events |
| `androidmanager.provider.Settings` | Read system/secure/global settings and open settings screens |
| `androidmanager.os.Build`, `androidmanager.os.Environment`, `androidmanager.content.Context`, `androidmanager.battery.Battery` | Build info, directories, logcat and battery |

## Example

```haxe
PermissionUtils.requestPermissions([Permission.READ_MEDIA_AUDIO], function(result) {
	if (result.denied.length > 0)
		Interface.showToast("Permission denied");
});

FileManager.pickDirectory(function(uri) {
	if (uri != null)
		FileManager.copyDirectoryAsync(uri, Context.getExternalFilesDir() + "/mods", function(count, error) {
			Interface.showToast(count + " files imported");
		});
});

Window.immersive = true;
Callback.interceptBackButton = true;
Callback.onBackPressed.add(function() Interface.showConfirm("Exit", "Close the game?", "Yes", "No", Application.exit));
```

The extension adds the `ACCESS_NETWORK_STATE` and `VIBRATE` permissions automatically. Define `ANDROID_MANAGER_NO_PERMISSIONS` to disable that. Notifications on Android 13+ require `POST_NOTIFICATIONS` in your project.xml.

Callbacks coming from Java are delivered on the Haxe main thread when using Lime 8 or newer.

## License
Android-Manager is free and anyone can use it, for more information, check [LICENSE](LICENSE.md)