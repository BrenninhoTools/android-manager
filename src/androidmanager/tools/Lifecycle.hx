package androidmanager.tools;

import androidmanager.callback.Callback;

class Lifecycle {
	private static var exitCallbacks:Array<Void->Void> = [];
	private static var resumeCallbacks:Array<Void->Void> = [];
	private static var destroyCallbacks:Array<Void->Void> = [];
	private static var initialized:Bool = false;

	public static var paused(get, never):Bool;

	public static function addOnExit(callback:Void->Void):Void {
		add(exitCallbacks, callback);
	}

	public static function removeOnExit(callback:Void->Void):Void {
		exitCallbacks.remove(callback);
	}

	public static function addOnResume(callback:Void->Void):Void {
		add(resumeCallbacks, callback);
	}

	public static function removeOnResume(callback:Void->Void):Void {
		resumeCallbacks.remove(callback);
	}

	public static function addOnDestroy(callback:Void->Void):Void {
		add(destroyCallbacks, callback);
	}

	public static function removeOnDestroy(callback:Void->Void):Void {
		destroyCallbacks.remove(callback);
	}

	public static function clear():Void {
		exitCallbacks = [];
		resumeCallbacks = [];
		destroyCallbacks = [];
	}

	private static function get_paused():Bool {
		return Callback.paused;
	}

	private static function add(list:Array<Void->Void>, callback:Void->Void):Void {
		if (callback == null)
			return;

		initIfNeeded();

		if (list.indexOf(callback) == -1)
			list.push(callback);
	}

	private static function initIfNeeded():Void {
		if (initialized)
			return;

		initialized = true;
		Callback.init();
		Callback.onPause.add(function() run(exitCallbacks));
		Callback.onResume.add(function() run(resumeCallbacks));
		Callback.onDestroy.add(function() run(destroyCallbacks));
	}

	private static function run(list:Array<Void->Void>):Void {
		for (callback in list.copy()) {
			if (callback == null)
				continue;

			try {
				callback();
			} catch (e:Dynamic) {}
		}
	}

	@:noCompletion
	public static function __dispatchExit():Void {
		run(exitCallbacks);
	}
}
