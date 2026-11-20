package androidmanager.os;

#if android
import androidmanager.jni.JNICache;
#end

enum abstract GameMode(Int) from Int to Int {
	var UNSUPPORTED = 0;
	var STANDARD = 1;
	var PERFORMANCE = 2;
	var BATTERY = 3;
	var CUSTOM = 4;
}

enum abstract GameStateMode(Int) from Int to Int {
	var UNKNOWN = 0;
	var NONE = 1;
	var GAMEPLAY_INTERRUPTIBLE = 2;
	var GAMEPLAY_UNINTERRUPTIBLE = 3;
	var CONTENT = 4;
}

class Performance {
	private static inline final CLASS:String = "androidmanager/PerformanceManager";

	public static var gameMode(get, never):GameMode;
	public static var hintSupported(get, never):Bool;
	public static var hintActive(get, never):Bool;
	public static var availableProcessors(get, never):Int;
	public static var sustainedPerformanceSupported(get, never):Bool;

	static var frameStart:Float = -1;

	public static function setGameState(loading:Bool, mode:GameStateMode = GAMEPLAY_INTERRUPTIBLE):Bool
		return invoke("setGameState", "(ZI)Z", [loading, mode], false);

	public static function startHintSession(targetFps:Float = 60):Bool
		return invoke("startHintSession", "(D)Z", [fpsToNanos(targetFps)], false);

	public static function setHintTarget(targetFps:Float):Void
		invoke("updateHintTarget", "(D)V", [fpsToNanos(targetFps)]);

	public static function reportWork(seconds:Float):Void
		invoke("reportHintDuration", "(D)V", [seconds * 1000000000.0]);

	public static inline function beginFrame():Void
		frameStart = haxe.Timer.stamp();

	public static function endFrame():Void {
		if (frameStart < 0)
			return;

		reportWork(haxe.Timer.stamp() - frameStart);
		frameStart = -1;
	}

	public static function closeHintSession():Void
		invoke("closeHintSession", "()V", []);

	public static function setCpuAwake(enable:Bool):Bool
		return invoke("setCpuAwake", "(Z)Z", [enable], false);

	static inline function fpsToNanos(fps:Float):Float
		return 1000000000.0 / Math.max(1, fps);

	private static function get_gameMode():GameMode
		return invoke("getGameMode", "()I", [], 0);

	private static function get_hintSupported():Bool
		return invoke("isHintSupported", "()Z", [], false);

	private static function get_hintActive():Bool
		return invoke("hasHintSession", "()Z", [], false);

	private static function get_availableProcessors():Int
		return invoke("getAvailableProcessors", "()I", [], 1);

	private static function get_sustainedPerformanceSupported():Bool
		return invoke("isSustainedPerformanceSupported", "()Z", [], false);

	private static function invoke(method:String, signature:String, args:Array<Dynamic>, ?fallback:Dynamic):Dynamic {
		#if android
		return JNICache.call(CLASS, method, signature, args, fallback);
		#else
		return fallback;
		#end
	}
}
