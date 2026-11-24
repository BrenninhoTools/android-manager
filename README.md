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
| `androidmanager.os.Tablet` | Form factor (phone, tablet, foldable, desktop, TV), window size classes, physical screen size, millimeter conversion, fold posture and hinge angle, change events |
| `androidmanager.view.Input` | Touch timings and slop, unbuffered touch, input device list, physical keyboard, mouse, stylus and gamepad detection, hiding the mouse pointer |
| `androidmanager.app.Application` | App info, open URLs, share, launch packages, custom intents, restart, exit |
| `androidmanager.app.Notification` | Channels, notifications with progress, permission request |
| `androidmanager.media.AudioManager` | Stream volume, audio focus events, headphones and output devices |
| `androidmanager.os.Vibrator` | Vibration patterns, amplitudes, predefined effects, haptic feedback |
| `androidmanager.content.FileManager` | File/folder pickers (SAF), copy between URIs and paths, list and import folders |
| `androidmanager.content.Interface` | Toasts, alerts, confirm, input, list, single/multi choice and progress dialogs |
| `androidmanager.content.Clipboard` | Read and write the clipboard |
| `androidmanager.net.Network` | Connection status, type, metered and change events |
| `androidmanager.games.PlayGames` | Google Play Games v2: sign in, player info and avatar, achievements, leaderboards, events, saved games |
| `androidmanager.content.Storage` | Volumes, SD card, space, game folder picker, public folders (Downloads, Documents...), async copy, move, delete, zip and unzip |
| `androidmanager.content.Assets` | Read, list and extract files from the APK assets, with multithreaded batch copies, progress and failed file reports |
| `androidmanager.content.Share` | Share files, multiple files and text, target specific apps and know which app was chosen |
| `androidmanager.view.Screenshot` | Screenshots to file, gallery, base64 or `Image`, crop, scale, share and screenshot detection |
| `androidmanager.provider.Settings` | Read system/secure/global settings and open settings screens |
| `androidmanager.os.Build`, `androidmanager.os.Environment`, `androidmanager.content.Context`, `androidmanager.battery.Battery` | Build info, directories (with `getFilesDir`, `getObbDir`, `getCacheDir` and `getExternalFilesDir` like extension-androidtools), logcat and battery |

## Java API

android-manager ships its own Java API in the `androidmanager` package, so other Lime extensions or custom Java code can use it directly.

```java
import androidmanager.AndroidManager;
import androidmanager.BuildInfo;
import androidmanager.Tools;

AndroidManager.addListener(new AndroidManager.Listener() {
    @Override
    public void onLifecycle(String state) {
        AndroidManager.d("State: " + state);
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }
});

if (AndroidManager.isAtLeast(33) && !Tools.isChromebook())
    Tools.makeToast(BuildInfo.getModel(), Tools.LENGTH_SHORT);
```

| Class | What it does |
| --- | --- |
| `AndroidManager` | Version, activity and context, UI thread and background helpers, logging, info JSON and `Listener` events |
| `BuildInfo` | Every `Build` and `Build.VERSION` value with API level guards, plus `toJson()` |
| `EnvironmentManager` | Standard directories and storage state, safe on every API level |
| `Tools` | Device checks, window, launch, URL and toast helpers |

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

## Tablets and foldables

```haxe
Tablet.onChange.add(function(info) layoutFor(info.widthClass, info.diagonalInches));
Tablet.onPostureChange.add(function(posture, angle) if (posture == HALF_OPENED) useTabletopLayout());
Tablet.onKeyboardChange.add(function(connected) showTouchControls(!connected));
Tablet.startListening();

if (Tablet.tablet || Tablet.isAtLeast(EXPANDED))
	useWideLayout();

var buttonSize = Tablet.millimetersToPixels(12);

Input.setPointerVisible(false);
for (device in Input.getExternalDevices())
	showDevice(device.name, device.types);
```

`Tablet.formFactor` is one of `phone`, `tablet`, `foldable`, `desktop` (Chromebook, Samsung DeX and PC mode), `tv`, `watch` and `automotive`. Width classes follow the Material window size classes (`compact` under 600dp, `medium` under 840dp, `expanded` under 1200dp, `large` under 1600dp, `extra_large` above). `millimetersToPixels` uses the real panel density, so controls keep the same physical size on phones and tablets. Posture and hinge angle need a foldable with a hinge sensor (Android 11+), and the events fire on fold, unfold, multi-window resize, rotation and keyboard changes.

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
Screenshot.share({maxSize: 1280}, {text: "My new high score!"});
Screenshot.shareTo(WHATSAPP, "Look at this");
Screenshot.captureImage({scale: 0.25}, function(image, ?error) if (image != null) preview.pixels = BitmapData.fromImage(image));

Screenshot.onUserScreenshot.add(function() Interface.makeToast("Screenshot detected"));
Screenshot.startDetection();
Screenshot.blocked = true;
```

Options: `format` (png, jpg, webp), `quality`, `scale`, `maxSize`, crop with `x`, `y`, `width` and `height`, `gallery`, `album`, `fileName`, `base64`, `sound` and `delay` in milliseconds. Detecting screenshots taken by the player requires Android 14+.

## Storage

```haxe
var dir = Storage.getGameDirectory("MyGame");
var mods = Storage.getPath(MEDIA, "mods");
var space = Storage.getSpace(dir);
Interface.makeToast(Storage.formatBytes(space.available) + " free");

var card = Storage.sdCard;
Storage.requestAccess(true, function(granted) if (granted) loadFrom(Storage.getPath(ROOT, ".MyGame")));

Storage.unzip(downloadedZip, mods, true, {
	onProgress: function(done, total, current) bar.value = done / total,
	onComplete: function(success, result) if (success) reloadMods()
});

Storage.zip(dir + "/saves", Storage.getPath(CACHE, "backup.zip"), {
	onComplete: function(success, path) if (success) Storage.saveToDownloads(path, "MyGame")
});
```

`getGameDirectory` picks the first writable folder between `/storage/emulated/0/.<name>` (only with `preferRoot` and all files access), `Android/media/<package>`, `Android/data/<package>/files` and the internal files folder. Locations: `INTERNAL`, `CACHE`, `EXTERNAL`, `EXTERNAL_CACHE`, `MEDIA`, `OBB`, `ROOT`. `saveToPublic` writes to Downloads, Documents, Pictures, Music, Movies or DCIM through MediaStore, without permissions on Android 10+. Async tasks: `copy`, `move`, `delete`, `zip`, `unzip`, `getSizeAsync`.

## Share

```haxe
Share.file(Storage.getPath(CACHE, "backup.zip"), {title: "Send backup"});
Share.files([chartPath, songPath], {target: DISCORD, text: "New chart"});
Share.text("Play my mod!", {target: WHATSAPP});
Share.onShared.add(function(app) Interface.makeToast("Shared with " + app));
```

Any file inside the app folders, `Android/media` or the shared storage can be shared. The extension includes its own read only content provider, so no `FileProvider` setup is needed. If the target app is not installed, the system share sheet opens instead, and the callback tells which app the player picked.

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

## Migrating from extension-androidtools

| extension-androidtools | android-manager |
| --- | --- |
| `extension.androidtools.content.Context.getFilesDir()` | `androidmanager.content.Context.getFilesDir()` |
| `extension.androidtools.content.Context.getExternalFilesDir()` | `androidmanager.content.Context.getExternalFilesDir()` |
| `extension.androidtools.content.Context.getObbDir()` | `androidmanager.content.Context.getObbDir()` |
| `extension.androidtools.content.Context.getCacheDir()` | `androidmanager.content.Context.getCacheDir()` |
| `extension.androidtools.os.Build` / `Build.VERSION` / `Build.VERSION_CODES` | `androidmanager.os.Build` / `Build.VERSION` / `Build.VERSION_CODES` |
| `extension.androidtools.Tools.isTablet()` | `androidmanager.Tools.isTablet()` or `androidmanager.os.Device.tablet` |
| `extension.androidtools.Tools.isChromebook()` | `androidmanager.Tools.isChromebook()` or `androidmanager.os.Device.chromebook` |
| `extension.androidtools.Tools.isRooted()` | `androidmanager.Tools.isRooted()` or `androidmanager.os.Device.rooted` |
| `extension.androidtools.widget.Toast.makeText(text, Toast.LENGTH_SHORT)` | `androidmanager.widget.Toast.makeText(text, Toast.LENGTH_SHORT)` |
| `extension.androidtools.os.Environment.getExternalStorageDirectory()` | `androidmanager.os.Environment.getExternalStorageDirectory()` |
| `extension.androidtools.Permissions.requestPermissions(...)` | `androidmanager.tools.PermissionUtils.requestPermissions(...)` |
| `extension.androidtools.Settings.requestSetting(...)` | `androidmanager.tools.PermissionUtils.requestManageExternalStorage()` |
| `extension.androidtools.os.Build.VERSION.SDK_INT` | `androidmanager.os.Build.VERSION.SDK_INT` |

