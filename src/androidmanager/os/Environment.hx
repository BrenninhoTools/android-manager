package androidmanager.os;

#if android
import androidmanager.jni.JNICache;
#end

enum abstract LogLevel(String) to String {
	var VERBOSE = "V";
	var DEBUG = "D";
	var INFO = "I";
	var WARN = "W";
	var ERROR = "E";
	var FATAL = "F";
	var SILENT = "S";
}

class Environment {
	static inline final CLASS:String = "androidmanager/EnvironmentManager";

	public static var DIRECTORY_DOWNLOADS(get, never):String;
	public static var DIRECTORY_MUSIC(get, never):String;
	public static var DIRECTORY_PICTURES(get, never):String;
	public static var DIRECTORY_DOCUMENTS(get, never):String;
	public static var DIRECTORY_MOVIES(get, never):String;
	public static var DIRECTORY_DCIM(get, never):String;
	public static var DIRECTORY_ALARMS(get, never):String;
	public static var DIRECTORY_RINGTONES(get, never):String;
	public static var DIRECTORY_NOTIFICATIONS(get, never):String;
	public static var DIRECTORY_PODCASTS(get, never):String;
	public static var DIRECTORY_AUDIOBOOKS(get, never):String;
	public static var DIRECTORY_SCREENSHOTS(get, never):String;
	public static var DIRECTORY_RECORDINGS(get, never):String;

	public static inline final MEDIA_MOUNTED:String = "mounted";
	public static inline final MEDIA_MOUNTED_READ_ONLY:String = "mounted_ro";
	public static inline final MEDIA_REMOVED:String = "removed";
	public static inline final MEDIA_UNMOUNTED:String = "unmounted";
	public static inline final MEDIA_UNKNOWN:String = "unknown";

	static var directoryNames:Map<String, String> = new Map();

	static function directoryName(key:String):String {
		if (directoryNames.exists(key))
			return directoryNames.get(key);

		#if android
		var value:Null<String> = JNICache.call(CLASS, "getDirectoryName", "(Ljava/lang/String;)Ljava/lang/String;", [key], null);
		if (value != null && value.length > 0) {
			directoryNames.set(key, value);
			return value;
		}
		#end
		return "Unknown";
	}

	static function string(method:String, ?args:Array<Dynamic>, fallback:String = "Unknown"):String {
		#if android
		var signature = args != null && args.length > 0 ? "(Ljava/lang/String;)Ljava/lang/String;" : "()Ljava/lang/String;";
		var value:Null<String> = JNICache.call(CLASS, method, signature, args != null ? args : [], null);
		return value != null && value.length > 0 ? value : fallback;
		#else
		return fallback;
		#end
	}

	static function bool(method:String):Bool {
		#if android
		var value:Null<Bool> = JNICache.call(CLASS, method, "()Z", [], null);
		return value == true;
		#else
		return false;
		#end
	}

	static function get_DIRECTORY_DOWNLOADS():String
		return directoryName("DOWNLOADS");

	static function get_DIRECTORY_MUSIC():String
		return directoryName("MUSIC");

	static function get_DIRECTORY_PICTURES():String
		return directoryName("PICTURES");

	static function get_DIRECTORY_DOCUMENTS():String
		return directoryName("DOCUMENTS");

	static function get_DIRECTORY_MOVIES():String
		return directoryName("MOVIES");

	static function get_DIRECTORY_DCIM():String
		return directoryName("DCIM");

	static function get_DIRECTORY_ALARMS():String
		return directoryName("ALARMS");

	static function get_DIRECTORY_RINGTONES():String
		return directoryName("RINGTONES");

	static function get_DIRECTORY_NOTIFICATIONS():String
		return directoryName("NOTIFICATIONS");

	static function get_DIRECTORY_PODCASTS():String
		return directoryName("PODCASTS");

	static function get_DIRECTORY_AUDIOBOOKS():String
		return directoryName("AUDIOBOOKS");

	static function get_DIRECTORY_SCREENSHOTS():String
		return directoryName("SCREENSHOTS");

	static function get_DIRECTORY_RECORDINGS():String
		return directoryName("RECORDINGS");

	public static function getExternalStorageDirectory():String
		return string("getExternalStorageDirectory");

	public static function getExternalStoragePublicDirectory(type:String):String
		return string("getExternalStoragePublicDirectory", [type != null ? type : ""]);

	public static function getDataDirectory():String
		return string("getDataDirectory");

	public static function getRootDirectory():String
		return string("getRootDirectory");

	public static function getDownloadCacheDirectory():String
		return string("getDownloadCacheDirectory");

	public static function getStorageDirectory():String
		return string("getStorageDirectory", null, "/storage");

	public static function getExternalStorageState():String
		return string("getExternalStorageState", null, MEDIA_UNKNOWN);

	public static function isExternalStorageMounted():Bool
		return bool("isExternalStorageMounted");

	public static function isExternalStorageReadOnly():Bool
		return bool("isExternalStorageReadOnly");

	public static function isExternalStorageManager():Bool
		return bool("isExternalStorageManager");

	public static function isExternalStorageRemovable():Bool
		return bool("isExternalStorageRemovable");

	public static function isExternalStorageEmulated():Bool
		return bool("isExternalStorageEmulated");

	public static function isExternalStorageLegacy():Bool
		return bool("isExternalStorageLegacy");

	public static function dumpSystemLogs(level:LogLevel = DEBUG, tag:String = "*"):String {
		#if android
		try {
			var args = ["-d", "-v", "threadtime"];
			if (tag == "*")
				args.push('*:$level');
			else {
				args.push('$tag:$level');
				args.push("*:S");
			}

			var process = new sys.io.Process("logcat", args);
			var output = process.stdout.readAll().toString();
			var errorOutput = process.stderr.readAll().toString();
			process.close();

			if (output.length == 0 && errorOutput.length > 0)
				return "Error in logcat command: " + errorOutput;

			return output;
		} catch (e:Dynamic) {
			return "Error trying to read logcat: " + e;
		}
		#else
		return "Unknown";
		#end
	}
}
