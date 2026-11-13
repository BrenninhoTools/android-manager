package androidmanager.view;

import androidmanager.app.Application;
import androidmanager.content.Context;
#if android
import androidmanager.jni.JNICache;
#end
import haxe.Json;
import haxe.Timer;
import haxe.crypto.Base64;
import lime.app.Event;
import lime.graphics.Image;

enum abstract ScreenshotFormat(String) from String to String {
	var PNG = "png";
	var JPG = "jpg";
	var WEBP = "webp";
}

typedef ScreenshotOptions = {
	@:optional var format:ScreenshotFormat;
	@:optional var quality:Int;
	@:optional var scale:Float;
	@:optional var maxSize:Int;
	@:optional var x:Int;
	@:optional var y:Int;
	@:optional var width:Int;
	@:optional var height:Int;
	@:optional var gallery:Bool;
	@:optional var album:String;
	@:optional var fileName:String;
	@:optional var base64:Bool;
	@:optional var sound:Bool;
	@:optional var delay:Int;
}

typedef ScreenshotResult = {
	var path:Null<String>;
	var uri:Null<String>;
	var base64:Null<String>;
	var width:Int;
	var height:Int;
	var format:ScreenshotFormat;
}

class Screenshot {
	private static inline final CLASS:String = "androidmanager/Screenshot";

	public static var onCapture(default, null):Event<ScreenshotResult->Void> = new Event<ScreenshotResult->Void>();
	public static var onError(default, null):Event<String->Void> = new Event<String->Void>();
	public static var onUserScreenshot(default, null):Event<Void->Void> = new Event<Void->Void>();

	public static var supported(get, never):Bool;
	public static var busy(get, never):Bool;
	public static var detectionSupported(get, never):Bool;
	public static var detecting(default, null):Bool = false;
	public static var blocked(get, set):Bool;

	public static var defaultFormat:ScreenshotFormat = PNG;
	public static var defaultFolder:String = "screenshots";

	public static function capture(?path:String, ?options:ScreenshotOptions, ?callback:(result:Null<ScreenshotResult>, ?error:String) -> Void):Void {
		if (options == null)
			options = {};

		if (options.format == null)
			options.format = path != null ? formatFromPath(path) : defaultFormat;

		if (path == null && options.gallery != true && options.base64 != true)
			path = defaultPath(options.format);

		var delay = options.delay != null ? options.delay : 0;
		if (delay > 0) {
			Timer.delay(function() run(path, options, callback), delay);
			return;
		}

		run(path, options, callback);
	}

	public static function saveToGallery(?options:ScreenshotOptions, ?callback:(uri:Null<String>, ?error:String) -> Void):Void {
		if (options == null)
			options = {};

		options.gallery = true;
		capture(null, options, function(result, ?error) {
			if (callback != null)
				callback(result != null ? result.uri : null, error);
		});
	}

	public static function captureBase64(?options:ScreenshotOptions, callback:(data:Null<String>, ?error:String) -> Void):Void {
		if (options == null)
			options = {};

		options.base64 = true;
		capture(null, options, function(result, ?error) {
			if (callback != null)
				callback(result != null ? result.base64 : null, error);
		});
	}

	public static function captureImage(?options:ScreenshotOptions, callback:(image:Null<Image>, ?error:String) -> Void):Void {
		captureBase64(options, function(data, ?error) {
			if (callback == null)
				return;

			if (data == null) {
				callback(null, error);
				return;
			}

			try {
				var image = Image.fromBytes(Base64.decode(data));
				callback(image, image == null ? "Image could not be decoded" : null);
			} catch (e:Dynamic) {
				callback(null, Std.string(e));
			}
		});
	}

	public static function captureAndShare(chooserTitle:String = "Share", ?options:ScreenshotOptions, ?callback:(shared:Bool, ?error:String) -> Void):Void {
		if (options == null)
			options = {};

		options.gallery = true;
		capture(null, options, function(result, ?error) {
			var ok = result != null && result.uri != null && Application.shareUri(result.uri, mimeOf(result.format), chooserTitle);
			if (callback != null)
				callback(ok, ok ? null : (error != null ? error : "Screenshot could not be shared"));
		});
	}

	public static function startDetection():Bool {
		#if android
		detecting = JNICache.call(CLASS, "startDetection", "(Lorg/haxe/lime/HaxeObject;)Z", [new DetectionHandler()], false);
		return detecting;
		#else
		return false;
		#end
	}

	public static function stopDetection():Void {
		detecting = false;
		invoke("stopDetection", "()V", []);
	}

	public static function playShutterSound():Void
		invoke("playShutterSound", "()V", []);

	@:noCompletion
	public static function __result(success:Bool, content:String, callback:(Null<ScreenshotResult>, ?String) -> Void):Void {
		var data:Dynamic = null;
		try {
			data = Json.parse(content);
		} catch (e:Dynamic) {}

		if (!success || data == null) {
			var message:String = data != null && Reflect.hasField(data, "error") ? data.error : content;
			onError.dispatch(message);
			if (callback != null)
				callback(null, message);
			return;
		}

		var result:ScreenshotResult = data;
		onCapture.dispatch(result);
		if (callback != null)
			callback(result, null);
	}

	private static function run(path:Null<String>, options:ScreenshotOptions, callback:(Null<ScreenshotResult>, ?String) -> Void):Void {
		#if android
		invoke("capture", "(Ljava/lang/String;Ljava/lang/String;Lorg/haxe/lime/HaxeObject;)V", [
			path == null ? "" : path,
			Json.stringify(options),
			new CaptureHandler(callback)
		]);
		#else
		__result(false, '{"error":"Screenshots are only available on Android"}', callback);
		#end
	}

	private static function defaultPath(format:ScreenshotFormat):Null<String> {
		var base = Context.getExternalFilesDir();
		if (base == null)
			base = Context.internalFilesDir;
		if (base == null)
			return null;

		var date = Date.now();
		var stamp = DateTools.format(date, "%Y%m%d_%H%M%S") + "_" + Std.string(Std.int((Timer.stamp() * 1000) % 1000));
		return base + "/" + defaultFolder + "/Screenshot_" + stamp + "." + format;
	}

	private static function formatFromPath(path:String):ScreenshotFormat {
		var lower = path.toLowerCase();
		if (StringTools.endsWith(lower, ".jpg") || StringTools.endsWith(lower, ".jpeg"))
			return JPG;
		if (StringTools.endsWith(lower, ".webp"))
			return WEBP;
		return PNG;
	}

	private static function mimeOf(format:ScreenshotFormat):String {
		return switch (format) {
			case JPG: "image/jpeg";
			case WEBP: "image/webp";
			default: "image/png";
		}
	}

	private static function get_supported():Bool
		return invoke("isSupported", "()Z", [], false);

	private static function get_busy():Bool
		return invoke("isBusy", "()Z", [], false);

	private static function get_detectionSupported():Bool
		return invoke("isDetectionSupported", "()Z", [], false);

	private static function get_blocked():Bool
		return Window.secure;

	private static function set_blocked(value:Bool):Bool
		return Window.secure = value;

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
private class CaptureHandler #if (lime >= "8.0.0") implements lime.system.JNI.JNISafety #end {
	private var callback:(Null<ScreenshotResult>, ?String) -> Void;

	public function new(callback:(Null<ScreenshotResult>, ?String) -> Void) {
		this.callback = callback;
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onResult(success:Bool, data:String):Void {
		Screenshot.__result(success, data, callback);
	}
}

@:noCompletion
private class DetectionHandler #if (lime >= "8.0.0") implements lime.system.JNI.JNISafety #end {
	public function new() {}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onScreenCaptured():Void {
		Screenshot.onUserScreenshot.dispatch();
	}
}
#end
