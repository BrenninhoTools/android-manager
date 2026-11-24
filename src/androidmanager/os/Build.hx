package androidmanager.os;

#if android
import androidmanager.jni.JNICache;
#end
import haxe.Json;

class Build {
	static inline final CLASS:String = "androidmanager/BuildInfo";

	public static var MODEL(get, never):String;
	public static var MANUFACTURER(get, never):String;
	public static var DEVICE(get, never):String;
	public static var BOARD(get, never):String;
	public static var BRAND(get, never):String;
	public static var HARDWARE(get, never):String;
	public static var DISPLAY(get, never):String;
	public static var FINGERPRINT(get, never):String;
	public static var PRODUCT(get, never):String;
	public static var BOOTLOADER(get, never):String;
	public static var ID(get, never):String;
	public static var HOST(get, never):String;
	public static var USER(get, never):String;
	public static var TAGS(get, never):String;
	public static var TYPE(get, never):String;
	public static var SOC_MANUFACTURER(get, never):String;
	public static var SOC_MODEL(get, never):String;
	public static var TIME(get, never):Float;
	public static var SUPPORTED_ABIS(get, never):Array<String>;
	public static var IS_64_BIT(get, never):Bool;

	public static function string(method:String):String {
		#if android
		var value:Null<String> = JNICache.call(CLASS, method, "()Ljava/lang/String;", [], null);
		return value != null ? value : "Unknown";
		#else
		return "Unknown";
		#end
	}

	public static function int(method:String):Int {
		#if android
		var value:Null<Int> = JNICache.call(CLASS, method, "()I", [], null);
		return value != null ? value : 0;
		#else
		return 0;
		#end
	}

	public static function isAtLeast(sdk:Int):Bool
		return VERSION.SDK_INT >= sdk;

	public static function toJson():Dynamic {
		#if android
		var raw:Null<String> = JNICache.call(CLASS, "toJson", "()Ljava/lang/String;", [], null);
		if (raw != null) {
			try {
				return Json.parse(raw);
			} catch (e:Dynamic) {}
		}
		#end
		return {};
	}

	static function get_MODEL():String
		return string("getModel");

	static function get_MANUFACTURER():String
		return string("getManufacturer");

	static function get_DEVICE():String
		return string("getDevice");

	static function get_BOARD():String
		return string("getBoard");

	static function get_BRAND():String
		return string("getBrand");

	static function get_HARDWARE():String
		return string("getHardware");

	static function get_DISPLAY():String
		return string("getDisplay");

	static function get_FINGERPRINT():String
		return string("getFingerprint");

	static function get_PRODUCT():String
		return string("getProduct");

	static function get_BOOTLOADER():String
		return string("getBootloader");

	static function get_ID():String
		return string("getId");

	static function get_HOST():String
		return string("getHost");

	static function get_USER():String
		return string("getUser");

	static function get_TAGS():String
		return string("getTags");

	static function get_TYPE():String
		return string("getType");

	static function get_SOC_MANUFACTURER():String
		return string("getSocManufacturer");

	static function get_SOC_MODEL():String
		return string("getSocModel");

	static function get_TIME():Float {
		#if android
		var value:Null<Float> = JNICache.call(CLASS, "getTime", "()J", [], null);
		return value != null ? value : 0.0;
		#else
		return 0.0;
		#end
	}

	static function get_SUPPORTED_ABIS():Array<String> {
		var raw = string("getSupportedAbis");
		try {
			var parsed:Array<String> = Json.parse(raw);
			return parsed != null ? parsed : [];
		} catch (e:Dynamic) {
			return [];
		}
	}

	static function get_IS_64_BIT():Bool {
		#if android
		var value:Null<Bool> = JNICache.call(CLASS, "is64Bit", "()Z", [], null);
		return value == true;
		#else
		return false;
		#end
	}
}

class VERSION {
	public static var BASE_OS(get, never):String;
	public static var RELEASE(get, never):String;
	public static var SDK_INT(get, never):Int;
	public static var PREVIEW_SDK_INT(get, never):Int;
	public static var CODENAME(get, never):String;
	public static var INCREMENTAL(get, never):String;
	public static var SECURITY_PATCH(get, never):String;
	public static var MEDIA_PERFORMANCE_CLASS(get, never):Int;

	static var cachedSdk:Int = -1;

	static function get_BASE_OS():String
		return Build.string("getBaseOs");

	static function get_RELEASE():String
		return Build.string("getRelease");

	static function get_SDK_INT():Int {
		if (cachedSdk < 0)
			cachedSdk = Build.int("getSdkInt");
		return cachedSdk;
	}

	static function get_PREVIEW_SDK_INT():Int
		return Build.int("getPreviewSdkInt");

	static function get_CODENAME():String
		return Build.string("getCodename");

	static function get_INCREMENTAL():String
		return Build.string("getIncremental");

	static function get_SECURITY_PATCH():String
		return Build.string("getSecurityPatch");

	static function get_MEDIA_PERFORMANCE_CLASS():Int
		return Build.int("getMediaPerformanceClass");
}

class VERSION_CODES {
	public static inline var BASE = 1;
	public static inline var BASE_1_1 = 2;
	public static inline var CUPCAKE = 3;
	public static inline var DONUT = 4;
	public static inline var ECLAIR = 5;
	public static inline var ECLAIR_0_1 = 6;
	public static inline var ECLAIR_MR1 = 7;
	public static inline var FROYO = 8;
	public static inline var GINGERBREAD = 9;
	public static inline var GINGERBREAD_MR1 = 10;
	public static inline var HONEYCOMB = 11;
	public static inline var HONEYCOMB_MR1 = 12;
	public static inline var HONEYCOMB_MR2 = 13;
	public static inline var ICE_CREAM_SANDWICH = 14;
	public static inline var ICE_CREAM_SANDWICH_MR1 = 15;
	public static inline var JELLY_BEAN = 16;
	public static inline var JELLY_BEAN_MR1 = 17;
	public static inline var JELLY_BEAN_MR2 = 18;
	public static inline var KITKAT = 19;
	public static inline var KITKAT_WATCH = 20;
	public static inline var LOLLIPOP = 21;
	public static inline var LOLLIPOP_MR1 = 22;
	public static inline var M = 23;
	public static inline var N = 24;
	public static inline var N_MR1 = 25;
	public static inline var O = 26;
	public static inline var O_MR1 = 27;
	public static inline var P = 28;
	public static inline var Q = 29;
	public static inline var R = 30;
	public static inline var S = 31;
	public static inline var S_V2 = 32;
	public static inline var TIRAMISU = 33;
	public static inline var UPSIDE_DOWN_CAKE = 34;
	public static inline var VANILLA_ICE_CREAM = 35;
	public static inline var BAKLAVA = 36;
	public static inline var CUR_DEVELOPMENT = 10000;
}
