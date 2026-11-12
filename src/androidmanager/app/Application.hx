package androidmanager.app;

import androidmanager.callback.Callback;
#if android
import androidmanager.jni.JNICache;
#end
import haxe.Json;

typedef AppInfo = {
	var packageName:String;
	var label:String;
	var versionName:String;
	var versionCode:Float;
	var firstInstallTime:Float;
	var lastUpdateTime:Float;
	var targetSdk:Int;
	var minSdk:Int;
	var debuggable:Bool;
	var sourceDir:String;
	var nativeLibraryDir:String;
	var installer:String;
}

class Application {
	private static inline final CLASS:String = "java/androidmanager/AppManager";

	public static var info(get, never):Null<AppInfo>;
	public static var installer(get, never):String;
	public static var launchIntent(get, never):Null<IntentData>;

	public static function isPackageInstalled(packageName:String):Bool
		return invoke("isPackageInstalled", "(Ljava/lang/String;)Z", [packageName], false);

	public static function launchPackage(packageName:String, ?onReturn:ActivityResult->Void):Bool {
		var requestCode:Int = -1;
		if (onReturn != null) {
			requestCode = Callback.nextRequestCode();
			Callback.waitForActivityResult(requestCode, onReturn);
		}

		var result:Bool = invoke("launchPackage", "(Ljava/lang/String;I)Z", [packageName, requestCode], false);
		if (!result && requestCode != -1)
			Callback.cancelWait(requestCode);

		return result;
	}

	public static function openURL(url:String):Bool
		return invoke("openURL", "(Ljava/lang/String;)Z", [url], false);

	public static function openStorePage(?packageName:String):Bool
		return invoke("openStorePage", "(Ljava/lang/String;)Z", [packageName == null ? "" : packageName], false);

	public static function shareText(text:String, ?subject:String, chooserTitle:String = "Share"):Bool
		return invoke("shareText", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", [text, subject == null ? "" : subject, chooserTitle], false);

	public static function shareUri(uri:String, ?mimeType:String, chooserTitle:String = "Share"):Bool
		return invoke("shareUri", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", [uri, mimeType == null ? "" : mimeType, chooserTitle], false);

	public static function sendEmail(to:String, subject:String, body:String):Bool
		return invoke("sendEmail", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", [to, subject, body], false);

	public static function startIntent(action:String, ?data:String, ?mimeType:String, ?packageName:String, ?onResult:ActivityResult->Void):Bool {
		var requestCode:Int = -1;
		if (onResult != null) {
			requestCode = Callback.nextRequestCode();
			Callback.waitForActivityResult(requestCode, onResult);
		}

		var result:Bool = invoke("startIntent", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)Z", [
			action,
			data == null ? "" : data,
			mimeType == null ? "" : mimeType,
			packageName == null ? "" : packageName,
			requestCode
		], false);

		if (!result && requestCode != -1)
			Callback.cancelWait(requestCode);

		return result;
	}

	public static function restart():Void
		invoke("restart", "()V", []);

	public static function exit():Void
		invoke("exit", "()V", []);

	private static function get_info():Null<AppInfo>
		return parse(invoke("getAppInfo", "()Ljava/lang/String;", [], null));

	private static function get_installer():String
		return invoke("getInstallerPackage", "()Ljava/lang/String;", [], "");

	private static function get_launchIntent():Null<IntentData>
		return parse(invoke("getLaunchIntent", "()Ljava/lang/String;", [], null));

	private static function parse(content:String):Dynamic {
		if (content == null || content.length == 0)
			return null;

		try {
			return Json.parse(content);
		} catch (e:Dynamic) {
			return null;
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
