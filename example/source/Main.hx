package;

import androidmanager.app.Application;
import androidmanager.app.Notification;
import androidmanager.callback.Callback;
import androidmanager.content.Clipboard;
import androidmanager.content.FileManager;
import androidmanager.content.Interface;
import androidmanager.media.AudioManager;
import androidmanager.net.Network;
import androidmanager.os.Device;
import androidmanager.os.Vibrator;
import androidmanager.tools.Lifecycle;
import androidmanager.tools.Permission;
import androidmanager.tools.PermissionUtils;
import androidmanager.view.Window;
#if openfl
import openfl.display.Sprite;
#end

class Main #if openfl extends Sprite #end {
	public function new() {
		#if openfl
		super();
		#end

		Callback.init();

		Window.immersive = true;
		Window.keepScreenOn = true;
		Window.setLayoutInCutout(true);

		Lifecycle.addOnExit(onAppClosing);
		Lifecycle.addOnResume(onAppResumed);

		Callback.interceptBackButton = true;
		Callback.onBackPressed.add(onBack);

		Network.onChange.add(function(status) {
			Interface.makeToast("Network: " + status.type + (status.connected ? " (online)" : " (offline)"));
		});
		Network.startListening();

		PermissionUtils.requestPermissions([Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE], function(result) {
			Interface.showStyledToast("Granted: " + result.granted.length + " / Denied: " + result.denied.length);
		});

		var display = Device.getDisplayInfo();
		var memory = Device.getMemoryInfo();
		var info = Application.info;

		var lines:Array<String> = [
			"Device: " + Device.name,
			"Rooted: " + Device.rooted,
			"Emulator: " + Device.emulator,
			"Tablet: " + Device.tablet,
			"CPU cores: " + Device.cpuCores,
			"ABIs: " + Device.supportedAbis.join(", "),
			"Refresh rate: " + (display != null ? display.refreshRate : 0),
			"RAM free: " + (memory != null ? Math.round(memory.available / 1048576) : 0) + " MB",
			"Version: " + (info != null ? info.versionName : "?")
		];

		Interface.showDialog("android-manager", lines.join("\n"), {positive: "Menu", negative: "Close"}, function(button) {
			if (button == POSITIVE)
				openMenu();
		});
	}

	private function openMenu():Void {
		Interface.showList("Options", [
			"Vibrate",
			"Pick a file",
			"Pick a folder",
			"Copy device name",
			"Notification",
			"Volume 50%",
			"Ask name"
		], function(index) {
			switch (index) {
				case 0:
					Vibrator.vibratePattern([0, 80, 60, 120]);
				case 1:
					FileManager.pickFile(function(uris) {
						if (uris.length > 0) {
							var uriInfo = FileManager.getUriInfo(uris[0]);
							Interface.makeToast(uriInfo != null ? uriInfo.name : uris[0], LONG);
						}
					}, ["audio/*", "application/json"], true);
				case 2:
					FileManager.pickDirectory(function(uri) {
						if (uri != null)
							Interface.makeToast(FileManager.listDirectory(uri).length + " entries", LONG);
					});
				case 3:
					Clipboard.text = Device.name;
				case 4:
					Notification.requestPermission(function(enabled) {
						if (enabled)
							Notification.show(1, "android-manager", "Hello from Haxe!");
					});
				case 5:
					AudioManager.setStreamVolumePercent(0.5, MUSIC, SHOW_UI);
				case 6:
					Interface.showInput("Your name", null, function(text) {
						if (text != null)
							Interface.showToast("Hi, " + text + "!");
					}, "Name");
				default:
			}
		});
	}

	private function onBack():Void {
		Interface.showConfirm("Exit", "Close the app?", "Yes", "No", function() Application.exit());
	}

	private function onAppClosing():Void {
		Vibrator.haptic();
	}

	private function onAppResumed():Void {
		Interface.makeToast("Welcome back");
	}
}
