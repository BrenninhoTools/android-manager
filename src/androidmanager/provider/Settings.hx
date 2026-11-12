package androidmanager.provider;

import androidmanager.callback.Callback;
#if android
import androidmanager.jni.JNICache;
#end

enum abstract SettingsTable(Int) from Int to Int {
	var SYSTEM = 0;
	var SECURE = 1;
	var GLOBAL = 2;
}

enum abstract SettingsAction(String) from String to String {
	var SETTINGS = "android.settings.SETTINGS";
	var APPLICATION_DETAILS = "android.settings.APPLICATION_DETAILS_SETTINGS";
	var MANAGE_ALL_FILES = "android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION";
	var MANAGE_OVERLAY = "android.settings.action.MANAGE_OVERLAY_PERMISSION";
	var MANAGE_WRITE_SETTINGS = "android.settings.action.MANAGE_WRITE_SETTINGS";
	var MANAGE_UNKNOWN_APP_SOURCES = "android.settings.MANAGE_UNKNOWN_APP_SOURCES";
	var APP_NOTIFICATION = "android.settings.APP_NOTIFICATION_SETTINGS";
	var WIFI = "android.settings.WIFI_SETTINGS";
	var WIRELESS = "android.settings.WIRELESS_SETTINGS";
	var BLUETOOTH = "android.settings.BLUETOOTH_SETTINGS";
	var DISPLAY = "android.settings.DISPLAY_SETTINGS";
	var SOUND = "android.settings.SOUND_SETTINGS";
	var BATTERY_SAVER = "android.settings.BATTERY_SAVER_SETTINGS";
	var IGNORE_BATTERY_OPTIMIZATION = "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS";
	var INTERNAL_STORAGE = "android.settings.INTERNAL_STORAGE_SETTINGS";
	var LOCALE = "android.settings.LOCALE_SETTINGS";
	var DATE = "android.settings.DATE_SETTINGS";
	var ACCESSIBILITY = "android.settings.ACCESSIBILITY_SETTINGS";
	var DEVELOPER = "android.settings.APPLICATION_DEVELOPMENT_SETTINGS";
	var SECURITY = "android.settings.SECURITY_SETTINGS";
	var LOCATION = "android.settings.LOCATION_SOURCE_SETTINGS";
}

class Settings {
	private static inline final CLASS:String = "java/androidmanager/SettingsManager";

	public static function getString(name:String, table:SettingsTable = SYSTEM):Null<String>
		return invoke("getString", "(ILjava/lang/String;)Ljava/lang/String;", [table, name], null);

	public static function getInt(name:String, table:SettingsTable = SYSTEM, fallback:Int = 0):Int
		return invoke("getInt", "(ILjava/lang/String;I)I", [table, name, fallback], fallback);

	public static function getFloat(name:String, table:SettingsTable = SYSTEM, fallback:Float = 0):Float
		return invoke("getFloat", "(ILjava/lang/String;F)F", [table, name, fallback], fallback);

	public static function putSystemInt(name:String, value:Int):Bool
		return invoke("putSystemInt", "(Ljava/lang/String;I)Z", [name, value], false);

	public static function open(action:SettingsAction, withPackage:Bool = false, ?onReturn:Void->Void):Bool {
		var requestCode = Callback.nextRequestCode();
		if (onReturn != null)
			Callback.waitForActivityResult(requestCode, function(_) onReturn());

		var fullAction:String = action;
		if (fullAction.indexOf(".") == -1)
			fullAction = "android.settings." + fullAction;

		var result:Bool = invoke("open", "(Ljava/lang/String;IZ)Z", [fullAction, requestCode, withPackage], false);
		if (!result)
			Callback.cancelWait(requestCode);

		return result;
	}

	public static function openAppNotificationSettings(?onReturn:Void->Void):Bool {
		var requestCode = Callback.nextRequestCode();
		if (onReturn != null)
			Callback.waitForActivityResult(requestCode, function(_) onReturn());

		var result:Bool = invoke("openAppNotifications", "(I)Z", [requestCode], false);
		if (!result)
			Callback.cancelWait(requestCode);

		return result;
	}

	private static function invoke(method:String, signature:String, args:Array<Dynamic>, ?fallback:Dynamic):Dynamic {
		#if android
		return JNICache.call(CLASS, method, signature, args, fallback);
		#else
		return fallback;
		#end
	}
}
