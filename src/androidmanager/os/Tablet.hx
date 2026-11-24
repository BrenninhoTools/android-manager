package androidmanager.os;

#if android
import androidmanager.jni.JNICache;
#end
import haxe.Json;
import lime.app.Event;

enum abstract FormFactor(String) from String to String {
	var PHONE = "phone";
	var TABLET = "tablet";
	var FOLDABLE = "foldable";
	var DESKTOP = "desktop";
	var TV = "tv";
	var WATCH = "watch";
	var AUTOMOTIVE = "automotive";
}

enum abstract SizeClass(String) from String to String {
	var COMPACT = "compact";
	var MEDIUM = "medium";
	var EXPANDED = "expanded";
	var LARGE = "large";
	var EXTRA_LARGE = "extra_large";
}

enum abstract Posture(String) from String to String {
	var UNKNOWN = "unknown";
	var CLOSED = "closed";
	var HALF_OPENED = "half_opened";
	var FLAT = "flat";
}

typedef ScreenInfo = {
	var formFactor:FormFactor;
	var largeScreen:Bool;
	var foldable:Bool;
	var desktop:Bool;
	var touchscreen:Bool;
	var widthDp:Int;
	var heightDp:Int;
	var smallestWidthDp:Int;
	var widthClass:SizeClass;
	var heightClass:SizeClass;
	var screenWidth:Int;
	var screenHeight:Int;
	var windowWidth:Int;
	var windowHeight:Int;
	var density:Float;
	var densityDpi:Int;
	var xdpi:Float;
	var ydpi:Float;
	var diagonalInches:Float;
	var orientation:Int;
	var multiWindow:Bool;
	var physicalKeyboard:Bool;
	var hingeAngle:Float;
	var posture:Posture;
}

class Tablet {
	private static inline final CLASS:String = "androidmanager/Tablet";

	public static var onChange(default, null):Event<ScreenInfo->Void> = new Event<ScreenInfo->Void>();
	public static var onFormFactorChange(default, null):Event<FormFactor->Void> = new Event<FormFactor->Void>();
	public static var onSizeClassChange(default, null):Event<(SizeClass, SizeClass) -> Void> = new Event<(SizeClass, SizeClass) -> Void>();
	public static var onPostureChange(default, null):Event<(Posture, Float) -> Void> = new Event<(Posture, Float) -> Void>();
	public static var onKeyboardChange(default, null):Event<Bool->Void> = new Event<Bool->Void>();

	public static var listening(default, null):Bool = false;
	public static var last(default, null):Null<ScreenInfo> = null;

	public static var formFactor(get, never):FormFactor;
	public static var tablet(get, never):Bool;
	public static var largeScreen(get, never):Bool;
	public static var foldable(get, never):Bool;
	public static var desktop(get, never):Bool;
	public static var touchscreen(get, never):Bool;
	public static var hingeAngle(get, never):Float;
	public static var posture(get, never):Posture;
	public static var info(get, never):Null<ScreenInfo>;

	public static function startListening():Void {
		if (listening)
			return;

		listening = true;
		#if android
		JNICache.call(CLASS, "startListening", "(Lorg/haxe/lime/HaxeObject;)V", [new TabletHandler()]);
		#end
	}

	public static function stopListening():Void {
		if (!listening)
			return;

		listening = false;
		invoke("stopListening", "()V", []);
	}

	public static function millimetersToPixels(millimeters:Float):Float
		return invoke("millimetersToPixels", "(F)F", [millimeters], millimeters * 160 / 25.4);

	public static function pixelsToMillimeters(pixels:Float):Float {
		final unit = millimetersToPixels(1);
		return unit > 0 ? pixels / unit : 0;
	}

	public static function isAtLeast(widthClass:SizeClass):Bool {
		final current = info;
		if (current == null)
			return widthClass == COMPACT;

		return rank(current.widthClass) >= rank(widthClass);
	}

	static function rank(value:SizeClass):Int {
		return switch (value) {
			case COMPACT: 0;
			case MEDIUM: 1;
			case EXPANDED: 2;
			case LARGE: 3;
			case EXTRA_LARGE: 4;
			default: 0;
		}
	}

	@:noCompletion
	public static function __screen(content:String):Void {
		var current:ScreenInfo = null;
		try {
			current = Json.parse(content);
		} catch (e:Dynamic) {}

		if (current == null)
			return;

		final previous = last;
		last = current;

		if (previous == null || previous.formFactor != current.formFactor)
			onFormFactorChange.dispatch(current.formFactor);

		if (previous == null || previous.widthClass != current.widthClass || previous.heightClass != current.heightClass)
			onSizeClassChange.dispatch(current.widthClass, current.heightClass);

		if (previous == null || previous.physicalKeyboard != current.physicalKeyboard)
			onKeyboardChange.dispatch(current.physicalKeyboard);

		onChange.dispatch(current);
	}

	@:noCompletion
	public static function __posture(value:String, angle:Float):Void {
		onPostureChange.dispatch(value, angle);
	}

	private static function get_formFactor():FormFactor
		return invoke("getFormFactor", "()Ljava/lang/String;", [], PHONE);

	private static function get_tablet():Bool {
		final value = formFactor;
		return value == TABLET || value == FOLDABLE || value == DESKTOP;
	}

	private static function get_largeScreen():Bool
		return invoke("isLargeScreen", "()Z", [], false);

	private static function get_foldable():Bool
		return invoke("isFoldable", "()Z", [], false);

	private static function get_desktop():Bool
		return invoke("isDesktop", "()Z", [], false);

	private static function get_touchscreen():Bool
		return invoke("hasTouchscreen", "()Z", [], true);

	private static function get_hingeAngle():Float
		return invoke("getHingeAngle", "()F", [], -1.0);

	private static function get_posture():Posture
		return invoke("getPosture", "()Ljava/lang/String;", [], UNKNOWN);

	private static function get_info():Null<ScreenInfo> {
		final content:Null<String> = invoke("getScreenInfo", "()Ljava/lang/String;", [], null);
		if (content == null || content.length == 0)
			return last;

		try {
			return Json.parse(content);
		} catch (e:Dynamic) {
			return last;
		}
	}

	private static function invoke(method:String, signature:String, args:Array<Dynamic>, ?fallback:Dynamic):Dynamic {
		#if android
		return JNICache.call(CLASS, method, signature, args, fallback);
		#else
		return fallback;
		#end
	}
}

#if android
@:noCompletion
private class TabletHandler #if (lime >= "8.0.0") implements lime.system.JNI.JNISafety #end {
	public function new() {}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onScreenChanged(content:String):Void {
		Tablet.__screen(content);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onPostureChanged(value:String, angle:Float):Void {
		Tablet.__posture(value, angle);
	}
}
#end
