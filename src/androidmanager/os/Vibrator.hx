package androidmanager.os;

#if android
import androidmanager.jni.JNICache;
#end

enum abstract VibrationEffect(Int) from Int to Int {
	var CLICK = 0;
	var DOUBLE_CLICK = 1;
	var TICK = 2;
	var HEAVY_CLICK = 5;
}

enum abstract HapticFeedback(Int) from Int to Int {
	var LONG_PRESS = 0;
	var VIRTUAL_KEY = 1;
	var KEYBOARD_TAP = 3;
	var CLOCK_TICK = 4;
	var CONTEXT_CLICK = 6;
	var KEYBOARD_RELEASE = 7;
	var VIRTUAL_KEY_RELEASE = 8;
	var TEXT_HANDLE_MOVE = 9;
	var GESTURE_START = 12;
	var GESTURE_END = 13;
	var CONFIRM = 16;
	var REJECT = 17;
	var TOGGLE_ON = 21;
	var TOGGLE_OFF = 22;
	var GESTURE_THRESHOLD_ACTIVATE = 23;
	var GESTURE_THRESHOLD_DEACTIVATE = 24;
	var DRAG_START = 25;
	var SEGMENT_TICK = 26;
	var SEGMENT_FREQUENT_TICK = 27;
}

class Vibrator {
	private static inline final CLASS:String = "java/androidmanager/Vibration";

	public static var hasVibrator(get, never):Bool;
	public static var hasAmplitudeControl(get, never):Bool;

	public static function vibrate(duration:Int = 100, amplitude:Int = -1):Void
		invoke("vibrate", "(II)V", [duration, amplitude]);

	public static function vibratePattern(timings:Array<Int>, ?amplitudes:Array<Int>, repeat:Int = -1):Void {
		if (timings == null || timings.length == 0)
			return;

		invoke("vibratePattern", "(Ljava/lang/String;Ljava/lang/String;I)V", [
			timings.join(","),
			amplitudes == null ? "" : amplitudes.join(","),
			repeat
		]);
	}

	public static function vibrateEffect(effect:VibrationEffect):Void
		invoke("vibratePredefined", "(I)V", [effect]);

	public static function haptic(feedback:HapticFeedback = VIRTUAL_KEY):Void
		invoke("performHapticFeedback", "(I)V", [feedback]);

	public static function cancel():Void
		invoke("cancel", "()V", []);

	private static function get_hasVibrator():Bool
		return invoke("hasVibrator", "()Z", [], false);

	private static function get_hasAmplitudeControl():Bool
		return invoke("hasAmplitudeControl", "()Z", [], false);

	private static function invoke(method:String, signature:String, args:Array<Dynamic>, ?fallback:Dynamic):Dynamic {
		#if android
		return JNICache.call(CLASS, method, signature, args, fallback);
		#else
		return fallback;
		#end
	}
}
