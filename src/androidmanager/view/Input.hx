package androidmanager.view;

#if android
import androidmanager.jni.JNICache;
#end

enum abstract InputSource(Int) from Int to Int {
	var TOUCHSCREEN = 0x00001002;
	var MOUSE = 0x00002002;
	var STYLUS = 0x00004002;
	var KEYBOARD = 0x00000101;
	var GAMEPAD = 0x00000401;
	var JOYSTICK = 0x01000010;
	var TOUCHPAD = 0x00100008;
}

class Input {
	private static inline final CLASS:String = "androidmanager/InputTools";

	public static var touchSlop(get, never):Int;
	public static var doubleTapSlop(get, never):Int;
	public static var minFlingVelocity(get, never):Int;
	public static var maxFlingVelocity(get, never):Int;
	public static var tapTimeout(get, never):Int;
	public static var doubleTapTimeout(get, never):Int;
	public static var longPressTimeout(get, never):Int;
	public static var density(get, never):Float;
	public static var maxTouchPoints(get, never):Int;
	public static var hapticFeedbackEnabled(get, never):Bool;

	public static function requestUnbufferedDispatch(enable:Bool = true):Bool
		return invoke("requestUnbufferedDispatch", "(Z)Z", [enable], false);

	public static function hasSource(source:InputSource):Bool
		return invoke("hasSource", "(I)Z", [source], false);

	public static function hasGamepad():Bool
		return hasSource(GAMEPAD) || hasSource(JOYSTICK);

	public static function hasMouse():Bool
		return hasSource(MOUSE);

	public static function hasStylus():Bool
		return hasSource(STYLUS);

	public static function hasKeyboard():Bool
		return hasSource(KEYBOARD);

	public static function dpToPixels(dp:Float):Float
		return dp * density;

	public static function pixelsToDp(pixels:Float):Float
		return pixels / Math.max(0.01, density);

	private static function get_touchSlop():Int
		return invoke("getTouchSlop", "()I", [], 16);

	private static function get_doubleTapSlop():Int
		return invoke("getDoubleTapSlop", "()I", [], 100);

	private static function get_minFlingVelocity():Int
		return invoke("getMinFlingVelocity", "()I", [], 50);

	private static function get_maxFlingVelocity():Int
		return invoke("getMaxFlingVelocity", "()I", [], 8000);

	private static function get_tapTimeout():Int
		return invoke("getTapTimeout", "()I", [], 100);

	private static function get_doubleTapTimeout():Int
		return invoke("getDoubleTapTimeout", "()I", [], 300);

	private static function get_longPressTimeout():Int
		return invoke("getLongPressTimeout", "()I", [], 400);

	private static function get_density():Float
		return invoke("getDensity", "()F", [], 1.0);

	private static function get_maxTouchPoints():Int
		return invoke("getMaxTouchPoints", "()I", [], 1);

	private static function get_hapticFeedbackEnabled():Bool
		return invoke("isHapticFeedbackEnabled", "()Z", [], true);

	private static function invoke(method:String, signature:String, args:Array<Dynamic>, ?fallback:Dynamic):Dynamic {
		#if android
		return JNICache.call(CLASS, method, signature, args, fallback);
		#else
		return fallback;
		#end
	}
}
