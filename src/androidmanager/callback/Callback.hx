package androidmanager.callback;

#if android
import androidmanager.jni.JNICache;
#end
import haxe.Json;
import lime.app.Event;

using StringTools;

typedef IntentData = {
	var action:Null<String>;
	var type:Null<String>;
	var data:Null<String>;
	var flags:Int;
	var uris:Array<String>;
	var extras:Dynamic;
}

typedef ActivityResult = {
	var requestCode:Int;
	var resultCode:Int;
	var data:Null<IntentData>;
	@:optional var error:String;
}

typedef PermissionsResult = {
	var requestCode:Int;
	var granted:Array<String>;
	var denied:Array<String>;
	var permanentlyDenied:Array<String>;
}

enum abstract ResultCode(Int) from Int to Int {
	var OK = -1;
	var CANCELED = 0;
	var FIRST_USER = 1;
}

enum abstract LifecycleEvent(String) from String to String {
	var CREATE = "create";
	var START = "start";
	var RESTART = "restart";
	var RESUME = "resume";
	var PAUSE = "pause";
	var STOP = "stop";
	var DESTROY = "destroy";
}

enum abstract TrimMemoryLevel(Int) from Int to Int {
	var RUNNING_MODERATE = 5;
	var RUNNING_LOW = 10;
	var RUNNING_CRITICAL = 15;
	var UI_HIDDEN = 20;
	var BACKGROUND = 40;
	var MODERATE = 60;
	var COMPLETE = 80;
}

class Callback {
	private static inline final CLASS:String = "androidmanager/CallbackManager";

	public static var onActivityResult(default, null):Event<ActivityResult->Void> = new Event<ActivityResult->Void>();
	public static var onRequestPermissionsResult(default, null):Event<PermissionsResult->Void> = new Event<PermissionsResult->Void>();
	public static var onLifecycle(default, null):Event<LifecycleEvent->Void> = new Event<LifecycleEvent->Void>();
	public static var onStart(default, null):Event<Void->Void> = new Event<Void->Void>();
	public static var onRestart(default, null):Event<Void->Void> = new Event<Void->Void>();
	public static var onResume(default, null):Event<Void->Void> = new Event<Void->Void>();
	public static var onPause(default, null):Event<Void->Void> = new Event<Void->Void>();
	public static var onStop(default, null):Event<Void->Void> = new Event<Void->Void>();
	public static var onDestroy(default, null):Event<Void->Void> = new Event<Void->Void>();
	public static var onLowMemory(default, null):Event<Void->Void> = new Event<Void->Void>();
	public static var onTrimMemory(default, null):Event<TrimMemoryLevel->Void> = new Event<TrimMemoryLevel->Void>();
	public static var onNewIntent(default, null):Event<IntentData->Void> = new Event<IntentData->Void>();
	public static var onBackPressed(default, null):Event<Void->Void> = new Event<Void->Void>();

	public static var initialized(default, null):Bool = false;
	public static var interceptBackButton(get, set):Bool;
	public static var paused(get, never):Bool;

	private static var requestCodeCounter:Int = 0x4100;
	private static var activityHandlers:Map<Int, ActivityResult->Void> = new Map();
	private static var permissionHandlers:Map<Int, PermissionsResult->Void> = new Map();

	public static function init():Void {
		if (initialized)
			return;

		initialized = true;

		#if android
		var setListener:Dynamic = JNICache.getStaticMethod(CLASS, "setListener", "(Lorg/haxe/lime/HaxeObject;)V");
		if (setListener != null)
			setListener(new CallbackHandler());
		#end
	}

	public static function nextRequestCode():Int {
		requestCodeCounter++;
		if (requestCodeCounter > 0xFFFF)
			requestCodeCounter = 0x4101;

		return requestCodeCounter;
	}

	public static function waitForActivityResult(requestCode:Int, handler:ActivityResult->Void):Void {
		init();
		if (handler != null)
			activityHandlers.set(requestCode, handler);
	}

	public static function waitForPermissionsResult(requestCode:Int, handler:PermissionsResult->Void):Void {
		init();
		if (handler != null)
			permissionHandlers.set(requestCode, handler);
	}

	public static function cancelWait(requestCode:Int):Void {
		activityHandlers.remove(requestCode);
		permissionHandlers.remove(requestCode);
	}

	private static function get_interceptBackButton():Bool {
		#if android
		return JNICache.call(CLASS, "isInterceptingBack", "()Z", [], false);
		#else
		return false;
		#end
	}

	private static function set_interceptBackButton(value:Bool):Bool {
		init();
		#if android
		JNICache.call(CLASS, "setInterceptBack", "(Z)V", [value]);
		#end
		return value;
	}

	private static function get_paused():Bool {
		#if android
		return JNICache.call(CLASS, "isPaused", "()Z", [], false);
		#else
		return false;
		#end
	}

	@:noCompletion
	public static function __activityResult(content:String):Void {
		var result:ActivityResult = parse(content);
		if (result == null)
			return;

		if (result.data != null && result.data.uris == null)
			result.data.uris = [];

		var handler = activityHandlers.get(result.requestCode);
		if (handler != null) {
			activityHandlers.remove(result.requestCode);
			handler(result);
		}

		onActivityResult.dispatch(result);
	}

	@:noCompletion
	public static function __permissionsResult(content:String):Void {
		var result:PermissionsResult = parse(content);
		if (result == null)
			return;

		var handler = permissionHandlers.get(result.requestCode);
		if (handler != null) {
			permissionHandlers.remove(result.requestCode);
			handler(result);
		}

		onRequestPermissionsResult.dispatch(result);
	}

	@:noCompletion
	public static function __lifecycle(event:String):Void {
		var value:LifecycleEvent = event;
		onLifecycle.dispatch(value);

		switch (value) {
			case START:
				onStart.dispatch();
			case RESTART:
				onRestart.dispatch();
			case RESUME:
				onResume.dispatch();
			case PAUSE:
				onPause.dispatch();
			case STOP:
				onStop.dispatch();
			case DESTROY:
				onDestroy.dispatch();
			default:
		}
	}

	@:noCompletion
	public static function __newIntent(content:String):Void {
		var data:IntentData = parse(content);
		if (data != null)
			onNewIntent.dispatch(data);
	}

	@:noCompletion
	public static function __lowMemory():Void {
		onLowMemory.dispatch();
	}

	@:noCompletion
	public static function __trimMemory(level:Int):Void {
		onTrimMemory.dispatch(level);
	}

	@:noCompletion
	public static function __backPressed():Void {
		onBackPressed.dispatch();
	}

	private static function parse(content:String):Dynamic {
		if (content == null)
			return null;

		try {
			return Json.parse(content.trim());
		} catch (e:Dynamic) {
			return null;
		}
	}
}

#if android
@:noCompletion
private class CallbackHandler #if (lime >= "8.0.0") implements lime.system.JNI.JNISafety #end {
	public function new() {}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onActivityResult(content:String):Void {
		Callback.__activityResult(content);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onRequestPermissionsResult(content:String):Void {
		Callback.__permissionsResult(content);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onLifecycle(event:String):Void {
		Callback.__lifecycle(event);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onNewIntent(content:String):Void {
		Callback.__newIntent(content);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onLowMemory():Void {
		Callback.__lowMemory();
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onTrimMemory(level:Int):Void {
		Callback.__trimMemory(level);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onBackPressed():Void {
		Callback.__backPressed();
	}
}
#end
