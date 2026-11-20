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

enum abstract VibrationPrimitive(Int) from Int to Int {
	var CLICK = 1;
	var THUD = 2;
	var SPIN = 3;
	var QUICK_RISE = 4;
	var SLOW_RISE = 5;
	var QUICK_FALL = 6;
	var TICK = 7;
	var LOW_TICK = 8;
}

enum abstract VibrationUsage(Int) from Int to Int {
	var DEFAULT = -1;
	var TOUCH = 0;
	var MEDIA = 1;
	var ALARM = 2;
	var NOTIFICATION = 3;
	var HARDWARE_FEEDBACK = 4;
}

typedef CompositionStep = {
	var primitive:VibrationPrimitive;
	@:optional var scale:Float;
	@:optional var delay:Int;
}

class Vibrator {
	private static inline final CLASS:String = "androidmanager/Vibration";

	public static var hasVibrator(get, never):Bool;
	public static var hasAmplitudeControl(get, never):Bool;
	public static var usage(get, set):VibrationUsage;
	public static var resonantFrequency(get, never):Float;
	public static var qFactor(get, never):Float;
	public static var vibratorCount(get, never):Int;

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

	public static function compose(steps:Array<CompositionStep>):Bool {
		if (steps == null || steps.length == 0)
			return false;

		return invoke("compose", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", [
			[for (step in steps) Std.string((step.primitive : Int))].join(","),
			[for (step in steps) Std.string(step.scale ?? 1.0)].join(","),
			[for (step in steps) Std.string(step.delay ?? 0)].join(",")
		], false);
	}

	public static function isPrimitiveSupported(primitive:VibrationPrimitive):Bool
		return invoke("isPrimitiveSupported", "(I)Z", [primitive], false);

	public static function isEffectSupported(effect:VibrationEffect):Bool
		return invoke("isEffectSupported", "(I)Z", [effect], false);

	public static function primitiveDuration(primitive:VibrationPrimitive):Int
		return invoke("getPrimitiveDuration", "(I)I", [primitive], -1);

	public static function cancel():Void
		invoke("cancel", "()V", []);

	private static function get_hasVibrator():Bool
		return invoke("hasVibrator", "()Z", [], false);

	private static function get_usage():VibrationUsage
		return invoke("getUsage", "()I", [], -1);

	private static function set_usage(value:VibrationUsage):VibrationUsage {
		invoke("setUsage", "(I)V", [value]);
		return value;
	}

	private static function get_resonantFrequency():Float
		return invoke("getResonantFrequency", "()F", [], -1.0);

	private static function get_qFactor():Float
		return invoke("getQFactor", "()F", [], -1.0);

	private static function get_vibratorCount():Int
		return invoke("getVibratorCount", "()I", [], 0);

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
