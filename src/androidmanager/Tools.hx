package androidmanager;

import androidmanager.app.Application;
import androidmanager.os.Device;
import androidmanager.view.Window;

class Tools {
	public static function isRooted():Bool
		return Device.rooted;

	public static function isEmulator():Bool
		return Device.emulator;

	public static function isTablet():Bool
		return Device.tablet;

	public static function isAndroidTV():Bool
		return Device.androidTV;

	public static function isChromebook():Bool
		return Device.chromebook;

	public static function isDeXMode():Bool
		return Device.dexMode;

	public static function isDolbyAtmos():Bool
		return Device.dolbyAtmos;

	public static function minimizeWindow():Void
		Window.minimize();

	public static function launchPackage(packageName:String):Bool
		return Application.launchPackage(packageName);

	public static function openURL(url:String):Bool
		return Application.openURL(url);
}
