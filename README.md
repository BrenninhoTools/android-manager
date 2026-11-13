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
| `androidmanager.games.PlayGames` | Google Play Games v2: sign in, player info and avatar, achievements, leaderboards, events, saved games |
| `androidmanager.view.Screenshot` | Screenshots to file, gallery, base64 or `Image`, crop, scale, share and screenshot detection |
| `androidmanager.provider.Settings` | Read system/secure/global settings and open settings screens |
| `androidmanager.os.Build`, `androidmanager.os.Environment`, `androidmanager.content.Context`, `androidmanager.battery.Battery` | Build info, directories, logcat and battery |

## Google Play Games

Play Games Services v2 is optional. Enable it by setting your Play Games app ID before including the library:

```xml
<set name="PLAY_GAMES_APP_ID" value="123456789012" />
<config:android minimum-sdk-version="23" />
<haxelib name="android-manager" />
```

Optionally pin the SDK version with `<set name="PLAY_GAMES_VERSION" value="22.1.0" />`.

### Connection modes

| `PLAY_GAMES_SIGN_IN` | Behavior at startup |
| --- | --- |
| `auto` (default) | Checks the silent sign in, and if the player is not signed in shows the sign in prompt automatically |
| `silent` | Only checks the silent sign in, never shows a prompt |
| `manual` | Does nothing until you call `PlayGames.connect()` |

```xml
<set name="PLAY_GAMES_SIGN_IN" value="auto" />
<set name="PLAY_GAMES_AUTO_PROMPTS" value="1" />
```

If the player cancels the automatic prompt, it is not shown again on the next launches (`PlayGames.rememberDecline`), but `PlayGames.connect()` always works and clears that. The connection is checked again silently every time the game comes back from the background (`PlayGames.checkOnResume`).

```haxe
PlayGames.onStateChange.add(function(state, error) {
	switch (state) {
		case CONNECTED: signInButton.visible = false;
		case FAILED | DISCONNECTED: signInButton.visible = true;
		default:
	}
});

signInButton.onClick = function() PlayGames.connect();
```

```haxe
PlayGames.init(function(signedIn) {
	if (!signedIn)
		PlayGames.signIn();
});

PlayGames.onSignInChanged.add(function(signedIn) {
	if (signedIn)
		PlayGames.getCurrentPlayer(function(player, ?error) Interface.showToast("Hi, " + player.displayName));
});

PlayGames.unlockAchievement("CgkI...");
PlayGames.submitScore("CgkI...", 15000);
PlayGames.showLeaderboard("CgkI...");
PlayGames.saveGame("slot1", Json.stringify(save), "Week 3");
PlayGames.loadGame("slot1", function(data, ?error) if (data != null) save = Json.parse(data));
```

## Battery

```haxe
Battery.onChange.add(function(info) {
	batteryText.text = info.level + "% " + info.chargeType + " " + info.temperature + "C " + info.powerWatts + "W";
});
Battery.onPowerConnected.add(function(connected) Interface.makeToast(connected ? "Charging" : "Unplugged"));
Battery.onThermalChange.add(function(status) if (status >= Severe) FlxG.updateFramerate = 30);
Battery.addLevelAlert(15, function(level) Interface.showToast("Battery at " + level + "%"));
Battery.startListening();

var health = Battery.getHealthPercent();
var cycles = Battery.getCycleCount();
```

`getDetails()` returns level with decimals, status, health, charge type, temperature, voltage, current (now and average), power in watts, charge and energy counters, design and estimated capacity, cycle count (Android 14+), time to full, time to empty (Android 12+), thermal status, power save and doze state.

## Window

```haxe
Window.immersive = true;
Window.setHighRefreshRate(true);
Window.setLayoutInCutout(true);

Window.onResize.add(function(width, height) resizeGame(width, height));
Window.onKeyboardChange.add(function(visible, height) moveTextField(visible ? height : 0));
Window.onFocusChange.add(function(focused) if (!focused) pauseGame());
Window.startListening();

var safe = Window.getSafeArea();
Window.captureScreen(Context.getExternalFilesDir() + "/screenshot.png", function(path, ?error) {});
```

Immersive mode is reapplied automatically when the window regains focus (`Window.autoReapplyImmersive`).

## Audio

```haxe
AudioManager.autoHandle(
	function() FlxG.sound.pause(),
	function() FlxG.sound.resume(),
	function(volume) FlxG.sound.volume = volume
);

AudioManager.onVolumeChange.add(function(stream, volume, max) volumeBar.value = volume / max);
AudioManager.onDeviceConnected.add(function(device) Interface.makeToast(device.name + " connected"));
AudioManager.onHeadphonesChange.add(function(connected) useHeadphoneMix(connected));

var info = AudioManager.info;
var latency = AudioManager.outputLatency;
```

`autoHandle` requests audio focus, makes the volume buttons control the game volume, pauses on focus loss, calls and when headphones are unplugged, resumes after transient losses and ducks the volume when another app plays a short sound.

## Screenshot

```haxe
Screenshot.capture();
Screenshot.capture(Context.getExternalFilesDir() + "/shot.jpg", {quality: 90, maxSize: 1280, sound: true});
Screenshot.saveToGallery({album: "My Game"}, function(uri, ?error) {});
Screenshot.captureAndShare("Share your score");
Screenshot.captureImage({scale: 0.25}, function(image, ?error) if (image != null) preview.pixels = BitmapData.fromImage(image));

Screenshot.onUserScreenshot.add(function() Interface.makeToast("Screenshot detected"));
Screenshot.startDetection();
Screenshot.blocked = true;
```

Options: `format` (png, jpg, webp), `quality`, `scale`, `maxSize`, crop with `x`, `y`, `width` and `height`, `gallery`, `album`, `fileName`, `base64`, `sound` and `delay` in milliseconds. Detecting screenshots taken by the player requires Android 14+.

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