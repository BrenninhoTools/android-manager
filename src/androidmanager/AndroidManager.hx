package androidmanager;

#if android
import androidmanager.jni.JNICache;
#end
import haxe.Json;

enum abstract LogPriority(Int) to Int {
	var VERBOSE = 2;
	var DEBUG = 3;
	var INFO = 4;
	var WARN = 5;
	var ERROR = 6;
}

class AndroidManager {
	public static inline final VERSION:String = "1.6.0";

	static inline final CLASS:String = "androidmanager/AndroidManager";

	public static var javaVersion(get, never):String;
	public static var ready(get, never):Bool;
	public static var paused(get, never):Bool;
	public static var packageName(get, never):String;
	public static var sdkInt(get, never):Int;
	public static var logging(get, set):Bool;

	static function get_javaVersion():String {
		#if android
		var value:Null<String> = JNICache.call(CLASS, "getVersion", "()Ljava/lang/String;", [], null);
		return value != null ? value : "Unknown";
		#else
		return "Unknown";
		#end
	}

	static function get_ready():Bool {
		#if android
		return JNICache.call(CLASS, "isReady", "()Z", [], false) == true;
		#else
		return false;
		#end
	}

	static function get_paused():Bool {
		#if android
		return JNICache.call(CLASS, "isPaused", "()Z", [], false) == true;
		#else
		return false;
		#end
	}

	static function get_packageName():String {
		#if android
		var value:Null<String> = JNICache.call(CLASS, "getPackageName", "()Ljava/lang/String;", [], null);
		return value != null ? value : "";
		#else
		return "";
		#end
	}

	static function get_sdkInt():Int
		return androidmanager.os.Build.VERSION.SDK_INT;

	static function get_logging():Bool {
		#if android
		return JNICache.call(CLASS, "isLogging", "()Z", [], false) == true;
		#else
		return false;
		#end
	}

	static function set_logging(value:Bool):Bool {
		#if android
		JNICache.call(CLASS, "setLogging", "(Z)V", [value]);
		#end
		return value;
	}

	public static function isAtLeast(sdk:Int):Bool
		return sdkInt >= sdk;

	public static function isJavaCompatible():Bool
		return javaVersion == VERSION;

	public static function log(message:String, priority:LogPriority = INFO):Void {
		#if android
		JNICache.call(CLASS, "log", "(ILjava/lang/String;)V", [priority, message]);
		#end
	}

	public static function info():Dynamic {
		#if android
		var raw:Null<String> = JNICache.call(CLASS, "getInfo", "()Ljava/lang/String;", [], null);
		if (raw != null) {
			try {
				return Json.parse(raw);
			} catch (e:Dynamic) {}
		}
		#end
		return {};
	}
}
