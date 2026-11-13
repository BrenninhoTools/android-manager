package androidmanager.view;

#if android
import androidmanager.jni.JNICache;
#end
import haxe.Json;
import lime.app.Event;

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

enum abstract ColorMode(Int) from Int to Int {
	var DEFAULT = 0;
	var WIDE_COLOR_GAMUT = 1;
	var HDR = 2;
}

typedef Insets = {
	var top:Int;
	var bottom:Int;
	var left:Int;
	var right:Int;
}

typedef WindowInfo = {
	var width:Int;
	var height:Int;
	var focused:Bool;
	var orientation:ConfigurationOrientation;
	var rotation:Int;
	var density:Float;
	var immersive:Bool;
	var multiWindow:Bool;
	var pictureInPicture:Bool;
	var statusBars:Insets;
	var navigationBars:Insets;
	var displayCutout:Insets;
	var systemGestures:Insets;
	var ime:Insets;
	var statusBarVisible:Bool;
	var navigationBarVisible:Bool;
	var keyboardVisible:Bool;
	var keyboardHeight:Int;
}

typedef DisplayMode = {
	var id:Int;
	var width:Int;
	var height:Int;
	var refreshRate:Float;
	var current:Bool;
}

class Window {
	private static inline final CLASS:String = "androidmanager/WindowManager";

	public static var secure(get, set):Bool;
	public static var keepScreenOn(get, set):Bool;
	public static var immersive(get, set):Bool;
	public static var orientation(get, set):ScreenOrientation;
	public static var currentOrientation(get, never):ConfigurationOrientation;
	public static var brightness(get, set):Float;

	public static var onChange(default, null):Event<WindowInfo->Void> = new Event<WindowInfo->Void>();
	public static var onResize(default, null):Event<(Int, Int) -> Void> = new Event<(Int, Int) -> Void>();
	public static var onOrientationChange(default, null):Event<ConfigurationOrientation->Void> = new Event<ConfigurationOrientation->Void>();
	public static var onFocusChange(default, null):Event<Bool->Void> = new Event<Bool->Void>();
	public static var onKeyboardChange(default, null):Event<(Bool, Int) -> Void> = new Event<(Bool, Int) -> Void>();
	public static var onInsetsChange(default, null):Event<WindowInfo->Void> = new Event<WindowInfo->Void>();

	public static var info(get, never):Null<WindowInfo>;
	public static var last(default, null):Null<WindowInfo> = null;
	public static var listening(default, null):Bool = false;
	public static var maxRefreshRate(get, never):Float;
	public static var wideColorGamut(get, never):Bool;
	public static var hdr(get, never):Bool;
	public static var autoReapplyImmersive(default, set):Bool = true;

	public static function startListening():Void {
		if (listening)
			return;

		listening = true;
		#if android
		invoke("startListening", "(Lorg/haxe/lime/HaxeObject;)V", [new WindowHandler()]);
		#end
	}

	public static function stopListening():Void {
		if (!listening)
			return;

		listening = false;
		invoke("stopListening", "()V", []);
	}

	public static function setSystemBarsVisible(statusBar:Bool, navigationBar:Bool):Void
		invoke("setSystemBarsVisible", "(ZZ)V", [statusBar, navigationBar]);

	public static function setLightSystemBars(lightStatusBar:Bool, lightNavigationBar:Bool):Void
		invoke("setLightSystemBars", "(ZZ)V", [lightStatusBar, lightNavigationBar]);

	public static function setEdgeToEdge(enabled:Bool):Void
		invoke("setEdgeToEdge", "(Z)V", [enabled]);

	public static function setShowWhenLocked(enabled:Bool):Void
		invoke("setShowWhenLocked", "(Z)V", [enabled]);

	public static function setTurnScreenOn(enabled:Bool):Void
		invoke("setTurnScreenOn", "(Z)V", [enabled]);

	public static function lockCurrentOrientation():Void
		invoke("lockCurrentOrientation", "()V", []);

	public static function getDisplayModes():Array<DisplayMode> {
		try {
			var result:Array<DisplayMode> = Json.parse(invoke("getDisplayModes", "()Ljava/lang/String;", [], "[]"));
			return result != null ? result : [];
		} catch (e:Dynamic) {
			return [];
		}
	}

	public static function setDisplayMode(modeId:Int):Void
		invoke("setDisplayMode", "(I)V", [modeId]);

	public static function setHighRefreshRate(enabled:Bool = true):Void
		invoke("setHighRefreshRate", "(Z)V", [enabled]);

	public static function setSurfaceFrameRate(rate:Float):Void
		invoke("setSurfaceFrameRate", "(F)V", [rate]);

	public static function setColorMode(mode:ColorMode):Void
		invoke("setColorMode", "(I)V", [mode]);

	public static function setDimAmount(amount:Float):Void
		invoke("setDimAmount", "(F)V", [amount]);

	public static function showKeyboard():Void
		invoke("showKeyboard", "()V", []);

	public static function hideKeyboard():Void
		invoke("hideKeyboard", "()V", []);

	public static function captureScreen(path:String, ?callback:(path:Null<String>, ?error:String) -> Void):Void {
		androidmanager.view.Screenshot.capture(path, null, function(result, ?error) {
			if (callback != null)
				callback(result != null ? result.path : null, error);
		});
	}

	public static function getSafeArea():Insets {
		var current = info;
		if (current == null)
			return {top: 0, bottom: 0, left: 0, right: 0};

		var sources = [current.displayCutout];
		if (current.statusBarVisible)
			sources.push(current.statusBars);
		if (current.navigationBarVisible)
			sources.push(current.navigationBars);

		var result:Insets = {top: 0, bottom: 0, left: 0, right: 0};
		for (source in sources) {
			if (source == null)
				continue;

			result.top = Std.int(Math.max(result.top, source.top));
			result.bottom = Std.int(Math.max(result.bottom, source.bottom));
			result.left = Std.int(Math.max(result.left, source.left));
			result.right = Std.int(Math.max(result.right, source.right));
		}
		return result;
	}

	@:noCompletion
	public static function __info(content:String):Void {
		var current:WindowInfo = null;
		try {
			current = Json.parse(content);
		} catch (e:Dynamic) {}

		if (current == null)
			return;

		var previous = last;
		last = current;

		if (previous == null || previous.width != current.width || previous.height != current.height)
			onResize.dispatch(current.width, current.height);

		if (previous == null || previous.orientation != current.orientation)
			onOrientationChange.dispatch(current.orientation);

		if (previous == null || Json.stringify([previous.statusBars, previous.navigationBars, previous.displayCutout, previous.ime]) != Json.stringify([current.statusBars, current.navigationBars, current.displayCutout, current.ime]))
			onInsetsChange.dispatch(current);

		onChange.dispatch(current);
	}

	private static function get_info():Null<WindowInfo> {
		try {
			return Json.parse(invoke("getWindowInfo", "()Ljava/lang/String;", [], "null"));
		} catch (e:Dynamic) {
			return null;
		}
	}

	private static function get_maxRefreshRate():Float
		return invoke("getMaxRefreshRate", "()F", [], 60.0);

	private static function get_wideColorGamut():Bool
		return invoke("isWideColorGamut", "()Z", [], false);

	private static function get_hdr():Bool
		return invoke("isHdr", "()Z", [], false);

	private static function set_autoReapplyImmersive(value:Bool):Bool {
		invoke("setAutoReapplyImmersive", "(Z)V", [value]);
		return autoReapplyImmersive = value;
	}

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

#if android
@:noCompletion
private class WindowHandler #if (lime >= "8.0.0") implements lime.system.JNI.JNISafety #end {
	public function new() {}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onWindowInfo(content:String):Void {
		Window.__info(content);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onFocusChanged(focused:Bool):Void {
		Window.onFocusChange.dispatch(focused);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onKeyboardChanged(visible:Bool, height:Int):Void {
		Window.onKeyboardChange.dispatch(visible, height);
	}
}

#end
