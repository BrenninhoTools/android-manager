package androidmanager.os;

#if android
import androidmanager.jni.JNICache;
#end
import haxe.Json;

typedef MemoryInfo = {
	var total:Float;
	var available:Float;
	var threshold:Float;
	var lowMemory:Bool;
	var memoryClass:Int;
	var largeMemoryClass:Int;
	var appMax:Float;
	var appTotal:Float;
	var appFree:Float;
	var appUsed:Float;
}

typedef StorageInfo = {
	var path:String;
	var total:Float;
	var free:Float;
	var available:Float;
}

typedef CutoutInsets = {
	var top:Int;
	var bottom:Int;
	var left:Int;
	var right:Int;
}

typedef DisplayInfo = {
	var width:Int;
	var height:Int;
	var appWidth:Int;
	var appHeight:Int;
	var density:Float;
	var densityDpi:Int;
	var scaledDensity:Float;
	var xdpi:Float;
	var ydpi:Float;
	var refreshRate:Float;
	var rotation:Int;
	var supportedRefreshRates:Array<Float>;
	var cutout:CutoutInsets;
	var hdr:Bool;
}

enum abstract ThermalStatus(Int) from Int to Int {
	var UNSUPPORTED = -1;
	var NONE = 0;
	var LIGHT = 1;
	var MODERATE = 2;
	var SEVERE = 3;
	var CRITICAL = 4;
	var EMERGENCY = 5;
	var SHUTDOWN = 6;
}

class Device {
	private static inline final CLASS:String = "java/androidmanager/Device";

	private static var rootedCache:Null<Bool> = null;
	private static var emulatorCache:Null<Bool> = null;

	public static var rooted(get, never):Bool;
	public static var emulator(get, never):Bool;
	public static var tablet(get, never):Bool;
	public static var androidTV(get, never):Bool;
	public static var watch(get, never):Bool;
	public static var automotive(get, never):Bool;
	public static var chromebook(get, never):Bool;
	public static var dexMode(get, never):Bool;
	public static var dolbyAtmos(get, never):Bool;
	public static var darkMode(get, never):Bool;
	public static var lowRamDevice(get, never):Bool;
	public static var multiWindow(get, never):Bool;
	public static var pictureInPicture(get, never):Bool;
	public static var thermalStatus(get, never):ThermalStatus;
	public static var cpuCores(get, never):Int;
	public static var supportedAbis(get, never):Array<String>;
	public static var glesVersion(get, never):String;
	public static var locale(get, never):String;
	public static var timeZone(get, never):String;
	public static var uptime(get, never):Float;
	public static var name(get, never):String;

	public static function isRooted(forceCheck:Bool = false):Bool {
		if (rootedCache == null || forceCheck)
			rootedCache = bool("isRooted");
		return rootedCache;
	}

	public static function isEmulator(forceCheck:Bool = false):Bool {
		if (emulatorCache == null || forceCheck)
			emulatorCache = bool("isEmulator");
		return emulatorCache;
	}

	public static function hasSystemFeature(feature:String):Bool {
		#if android
		return JNICache.call(CLASS, "hasSystemFeature", "(Ljava/lang/String;)Z", [feature], false);
		#else
		return false;
		#end
	}

	public static function getMemoryInfo():Null<MemoryInfo> {
		return json(string("getMemoryInfo"));
	}

	public static function getStorageInfo(?path:String):Null<StorageInfo> {
		#if android
		return json(JNICache.call(CLASS, "getStorageInfo", "(Ljava/lang/String;)Ljava/lang/String;", [path == null ? "" : path], null));
		#else
		return null;
		#end
	}

	public static function getDisplayInfo():Null<DisplayInfo> {
		return json(string("getDisplayInfo"));
	}

	private static function get_rooted():Bool
		return isRooted();

	private static function get_emulator():Bool
		return isEmulator();

	private static function get_tablet():Bool
		return bool("isTablet");

	private static function get_androidTV():Bool
		return bool("isAndroidTV");

	private static function get_watch():Bool
		return bool("isWatch");

	private static function get_automotive():Bool
		return bool("isAutomotive");

	private static function get_chromebook():Bool
		return bool("isChromebook");

	private static function get_dexMode():Bool
		return bool("isDeXMode");

	private static function get_dolbyAtmos():Bool
		return bool("isDolbyAtmos");

	private static function get_darkMode():Bool
		return bool("isDarkMode");

	private static function get_lowRamDevice():Bool
		return bool("isLowRamDevice");

	private static function get_multiWindow():Bool
		return bool("isInMultiWindowMode");

	private static function get_pictureInPicture():Bool
		return bool("isInPictureInPictureMode");

	private static function get_thermalStatus():ThermalStatus {
		#if android
		return JNICache.call(CLASS, "getThermalStatus", "()I", [], -1);
		#else
		return UNSUPPORTED;
		#end
	}

	private static function get_cpuCores():Int {
		#if android
		return JNICache.call(CLASS, "getCpuCores", "()I", [], 1);
		#else
		return 1;
		#end
	}

	private static function get_supportedAbis():Array<String> {
		var result:Array<String> = json(string("getSupportedAbis"));
		return result != null ? result : [];
	}

	private static function get_glesVersion():String
		return string("getGlEsVersion");

	private static function get_locale():String
		return string("getLocale");

	private static function get_timeZone():String
		return string("getTimeZone");

	private static function get_name():String
		return string("getDeviceName");

	private static function get_uptime():Float {
		#if android
		return JNICache.call(CLASS, "getUptime", "()D", [], 0.0);
		#else
		return 0;
		#end
	}

	private static function bool(method:String):Bool {
		#if android
		return JNICache.call(CLASS, method, "()Z", [], false);
		#else
		return false;
		#end
	}

	private static function string(method:String):String {
		#if android
		var value:Null<String> = JNICache.call(CLASS, method, "()Ljava/lang/String;", [], "");
		return value != null ? value : "";
		#else
		return "";
		#end
	}

	private static function json(content:String):Dynamic {
		if (content == null || content.length == 0)
			return null;

		try {
			return Json.parse(content);
		} catch (e:Dynamic) {
			return null;
		}
	}
}
