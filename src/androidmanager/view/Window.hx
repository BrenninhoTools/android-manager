package androidmanager.view;

#if android
import androidmanager.jni.JNICache;
#end

enum abstract ScreenOrientation(Int) from Int to Int {
	var UNSPECIFIED = -1;
	var LANDSCAPE = 0;
	var PORTRAIT = 1;
	var USER = 2;
	var BEHIND = 3;
	var SENSOR = 4;
	var NOSENSOR = 5;
	var SENSOR_LANDSCAPE = 6;
	var SENSOR_PORTRAIT = 7;
	var REVERSE_LANDSCAPE = 8;
	var REVERSE_PORTRAIT = 9;
	var FULL_SENSOR = 10;
	var USER_LANDSCAPE = 11;
	var USER_PORTRAIT = 12;
	var FULL_USER = 13;
	var LOCKED = 14;
}

enum abstract ConfigurationOrientation(Int) from Int to Int {
	var UNDEFINED = 0;
	var PORTRAIT = 1;
	var LANDSCAPE = 2;
}

class Window {
	private static inline final CLASS:String = "java/androidmanager/WindowManager";

	public static var secure(get, set):Bool;
	public static var keepScreenOn(get, set):Bool;
	public static var immersive(get, set):Bool;
	public static var orientation(get, set):ScreenOrientation;
	public static var currentOrientation(get, never):ConfigurationOrientation;
	public static var brightness(get, set):Float;

	public static function setLayoutInCutout(enabled:Bool):Void
		invoke("setLayoutInCutout", "(Z)V", [enabled]);

	public static function setPreferredRefreshRate(rate:Float):Void
		invoke("setPreferredRefreshRate", "(F)V", [rate]);

	public static function setSustainedPerformance(enabled:Bool):Void
		invoke("setSustainedPerformance", "(Z)V", [enabled]);

	public static function setStatusBarColor(color:Int):Void
		invoke("setStatusBarColor", "(I)V", [color]);

	public static function setNavigationBarColor(color:Int):Void
		invoke("setNavigationBarColor", "(I)V", [color]);

	public static function setTitle(title:String):Void
		invoke("setTitle", "(Ljava/lang/String;)V", [title]);

	public static function minimize():Void
		invoke("minimize", "()V", []);

	public static function resetBrightness():Void
		invoke("setBrightness", "(F)V", [-1.0]);

	private static function get_secure():Bool
		return invoke("isSecure", "()Z", [], false);

	private static function set_secure(value:Bool):Bool {
		invoke("setSecure", "(Z)V", [value]);
		return value;
	}

	private static function get_keepScreenOn():Bool
		return invoke("isKeepScreenOn", "()Z", [], false);

	private static function set_keepScreenOn(value:Bool):Bool {
		invoke("setKeepScreenOn", "(Z)V", [value]);
		return value;
	}

	private static function get_immersive():Bool
		return invoke("isImmersive", "()Z", [], false);

	private static function set_immersive(value:Bool):Bool {
		invoke("setImmersive", "(Z)V", [value]);
		return value;
	}

	private static function get_orientation():ScreenOrientation
		return invoke("getOrientation", "()I", [], -1);

	private static function set_orientation(value:ScreenOrientation):ScreenOrientation {
		invoke("setOrientation", "(I)V", [value]);
		return value;
	}

	private static function get_currentOrientation():ConfigurationOrientation
		return invoke("getCurrentOrientation", "()I", [], 0);

	private static function get_brightness():Float
		return invoke("getBrightness", "()F", [], -1.0);

	private static function set_brightness(value:Float):Float {
		invoke("setBrightness", "(F)V", [value]);
		return value;
	}

	private static function invoke(method:String, signature:String, args:Array<Dynamic>, ?fallback:Dynamic):Dynamic {
		#if android
		return JNICache.call(CLASS, method, signature, args, fallback);
		#else
		return fallback;
		#end
	}
}
