package androidmanager.content;

import androidmanager.callback.Callback;
#if android
import androidmanager.jni.JNICache;
#end
import haxe.Json;
import lime.app.Event;

enum abstract ShareTarget(String) from String to String {
	var ANY = "";
	var WHATSAPP = "com.whatsapp";
	var WHATSAPP_BUSINESS = "com.whatsapp.w4b";
	var INSTAGRAM = "com.instagram.android";
	var DISCORD = "com.discord";
	var TELEGRAM = "org.telegram.messenger";
	var TWITTER = "com.twitter.android";
	var FACEBOOK = "com.facebook.katana";
	var MESSENGER = "com.facebook.orca";
	var TIKTOK = "com.zhiliaoapp.musically";
	var REDDIT = "com.reddit.frontpage";
	var SNAPCHAT = "com.snapchat.android";
	var GMAIL = "com.google.android.gm";
	var DRIVE = "com.google.android.apps.docs";
	var PHOTOS = "com.google.android.apps.photos";
}

typedef ShareOptions = {
	@:optional var title:String;
	@:optional var text:String;
	@:optional var subject:String;
	@:optional var mimeType:String;
	@:optional var target:ShareTarget;
}

typedef ShareResult = {
	var started:Bool;
	var app:Null<String>;
	var error:Null<String>;
}

class Share {
	private static inline final CLASS:String = "androidmanager/ShareManager";

	public static var onShared(default, null):Event<String->Void> = new Event<String->Void>();

	private static var initialized:Bool = false;
	private static var chosen:Map<Int, String> = new Map();

	public static function file(path:String, ?options:ShareOptions, ?callback:ShareResult->Void):Void
		files([path], options, callback);

	public static function files(paths:Array<String>, ?options:ShareOptions, ?callback:ShareResult->Void):Void
		run(paths, options, callback);

	public static function text(text:String, ?options:ShareOptions, ?callback:ShareResult->Void):Void {
		if (options == null)
			options = {};

		options.text = text;
		run([], options, callback);
	}

	public static function getUriForFile(path:String):Null<String> {
		#if android
		return JNICache.call(CLASS, "getUriForFile", "(Ljava/lang/String;)Ljava/lang/String;", [path], null);
		#else
		return null;
		#end
	}

	@:noCompletion
	public static function __chosen(app:String, requestCode:Int):Void {
		if (requestCode >= 0)
			chosen.set(requestCode, app);

		onShared.dispatch(app);
	}

	private static function run(paths:Array<String>, options:ShareOptions, callback:ShareResult->Void):Void {
		if (options == null)
			options = {};

		#if android
		init();

		var requestCode = Callback.nextRequestCode();
		Callback.waitForActivityResult(requestCode, function(_) {
			var app = chosen.get(requestCode);
			chosen.remove(requestCode);
			if (callback != null)
				callback({started: true, app: app, error: null});
		});

		var error:String = JNICache.call(CLASS, "share",
			"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/String;", [
				Json.stringify(paths == null ? [] : paths),
				options.text == null ? "" : options.text,
				options.subject == null ? "" : options.subject,
				options.mimeType == null ? "" : options.mimeType,
				options.title == null ? "" : options.title,
				options.target == null ? "" : options.target,
				requestCode
			], "Share is not available");

		if (error != null && error.length > 0) {
			Callback.cancelWait(requestCode);
			if (callback != null)
				callback({started: false, app: null, error: error});
		}
		#else
		if (callback != null)
			callback({started: false, app: null, error: "Share is only available on Android"});
		#end
	}

	private static function init():Void {
		#if android
		if (initialized)
			return;

		initialized = true;
		JNICache.call(CLASS, "setListener", "(Lorg/haxe/lime/HaxeObject;)V", [new ShareHandler()]);
		#end
	}
}

#if android
@:noCompletion
private class ShareHandler #if (lime >= "8.0.0") implements lime.system.JNI.JNISafety #end {
	public function new() {}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onChosen(app:String, requestCode:Int):Void {
		Share.__chosen(app, requestCode);
	}
}
#end
