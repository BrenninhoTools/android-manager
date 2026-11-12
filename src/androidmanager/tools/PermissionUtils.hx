package androidmanager.tools;

import androidmanager.callback.Callback;
#if android
import androidmanager.jni.JNICache;
#end
import haxe.Json;

class PermissionUtils {
	private static inline final CLASS:String = "java/androidmanager/PermissionManager";

	public static function formatPermission(permission:String):Null<String> {
		if (permission == null)
			return null;

		permission = StringTools.trim(permission);
		if (permission.length == 0)
			return null;

		return permission.indexOf(".") != -1 ? permission : "android.permission." + permission;
	}

	public static function formatPermissions(permissions:Array<String>):Array<String> {
		var formatted:Array<String> = [];
		if (permissions == null)
			return formatted;

		for (permission in permissions) {
			var value = formatPermission(permission);
			if (value != null && formatted.indexOf(value) == -1)
				formatted.push(value);
		}

		return formatted;
	}

	public static function hasPermission(permission:String):Bool {
		#if android
		var value = formatPermission(permission);
		if (value == null)
			return false;

		return JNICache.call(CLASS, "hasPermission", "(Ljava/lang/String;)Z", [value], false);
		#else
		return false;
		#end
	}

	public static function hasPermissions(permissions:Array<String>):Bool {
		#if android
		var values = formatPermissions(permissions);
		if (values.length == 0)
			return false;

		return JNICache.call(CLASS, "hasPermissions", "([Ljava/lang/String;)Z", [values], false);
		#else
		return false;
		#end
	}

	public static function requestPermission(permission:String, ?callback:PermissionsResult->Void):Void {
		requestPermissions([permission], callback);
	}

	public static function requestPermissions(permissions:Array<String>, ?callback:PermissionsResult->Void):Void {
		var values = formatPermissions(permissions);
		if (values.length == 0)
			return;

		#if android
		var requestCode:Int = Callback.nextRequestCode();
		if (callback != null)
			Callback.waitForPermissionsResult(requestCode, callback);
		else
			Callback.init();

		var requested:Bool = JNICache.call(CLASS, "requestPermissionsWithCode", "([Ljava/lang/String;I)Z", [values, requestCode], false);
		if (requested)
			return;

		Callback.cancelWait(requestCode);

		if (callback != null) {
			var granted:Array<String> = [];
			var denied:Array<String> = [];
			for (value in values)
				(hasPermission(value) ? granted : denied).push(value);

			callback({
				requestCode: requestCode,
				granted: granted,
				denied: denied,
				permanentlyDenied: []
			});
		}
		#else
		if (callback != null)
			callback({
				requestCode: -1,
				granted: [],
				denied: values,
				permanentlyDenied: []
			});
		#end
	}

	public static function shouldShowRationale(permission:String):Bool {
		#if android
		var value = formatPermission(permission);
		return value != null && JNICache.call(CLASS, "shouldShowRationale", "(Ljava/lang/String;)Z", [value], false);
		#else
		return false;
		#end
	}

	public static function getRequestedPermissions():Array<String> {
		return parseArray(callString("getRequestedPermissions"));
	}

	public static function getGrantedPermissions():Array<String> {
		return parseArray(callString("getGrantedPermissions"));
	}

	public static function getDeniedPermissions():Array<String> {
		var granted = getGrantedPermissions();
		return getRequestedPermissions().filter(function(p) return granted.indexOf(p) == -1);
	}

	public static function isExternalStorageManager():Bool {
		return callBool("isExternalStorageManager");
	}

	public static function requestManageExternalStorage(?callback:Bool->Void):Void {
		requestSpecial("requestManageExternalStorage", isExternalStorageManager, callback);
	}

	public static function canDrawOverlays():Bool {
		return callBool("canDrawOverlays");
	}

	public static function requestDrawOverlays(?callback:Bool->Void):Void {
		requestSpecial("requestDrawOverlays", canDrawOverlays, callback);
	}

	public static function canWriteSettings():Bool {
		return callBool("canWriteSettings");
	}

	public static function requestWriteSettings(?callback:Bool->Void):Void {
		requestSpecial("requestWriteSettings", canWriteSettings, callback);
	}

	public static function canRequestPackageInstalls():Bool {
		return callBool("canRequestPackageInstalls");
	}

	public static function requestInstallPackages(?callback:Bool->Void):Void {
		requestSpecial("requestInstallPackages", canRequestPackageInstalls, callback);
	}

	public static function openAppSettings(?callback:Void->Void):Void {
		#if android
		var requestCode = Callback.nextRequestCode();
		if (callback != null)
			Callback.waitForActivityResult(requestCode, function(_) callback());

		JNICache.call(CLASS, "openAppSettings", "(I)Z", [requestCode], false);
		#end
	}

	private static function requestSpecial(method:String, check:Void->Bool, ?callback:Bool->Void):Void {
		#if android
		if (check()) {
			if (callback != null)
				callback(true);
			return;
		}

		var requestCode = Callback.nextRequestCode();
		var finish = function() {
			Callback.cancelWait(requestCode);
			if (callback != null)
				callback(check());
		};

		Callback.waitForActivityResult(requestCode, function(_) finish());
		Callback.waitForPermissionsResult(requestCode, function(_) finish());

		var started:Bool = JNICache.call(CLASS, method, "(I)Z", [requestCode], false);
		if (!started) {
			Callback.cancelWait(requestCode);
			finish();
		}
		#else
		if (callback != null)
			callback(false);
		#end
	}

	private static function callBool(method:String):Bool {
		#if android
		return JNICache.call(CLASS, method, "()Z", [], false);
		#else
		return false;
		#end
	}

	private static function callString(method:String):String {
		#if android
		return JNICache.call(CLASS, method, "()Ljava/lang/String;", [], "[]");
		#else
		return "[]";
		#end
	}

	private static function parseArray(content:String):Array<String> {
		try {
			var result:Array<String> = Json.parse(content);
			return result != null ? result : [];
		} catch (e:Dynamic) {
			return [];
		}
	}
}

typedef PermissionsResult = androidmanager.callback.Callback.PermissionsResult;