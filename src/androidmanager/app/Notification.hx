package androidmanager.app;

#if android
import androidmanager.jni.JNICache;
#end
import androidmanager.tools.PermissionUtils;

enum abstract NotificationImportance(Int) from Int to Int {
	var NONE = 0;
	var MIN = 1;
	var LOW = 2;
	var DEFAULT = 3;
	var HIGH = 4;
	var MAX = 5;
}

enum abstract NotificationPriority(Int) from Int to Int {
	var MIN = -2;
	var LOW = -1;
	var DEFAULT = 0;
	var HIGH = 1;
	var MAX = 2;
}

typedef NotificationOptions = {
	@:optional var channelId:String;
	@:optional var channelName:String;
	@:optional var priority:NotificationPriority;
	@:optional var autoCancel:Bool;
	@:optional var ongoing:Bool;
	@:optional var progressMax:Int;
	@:optional var progress:Int;
	@:optional var indeterminate:Bool;
	@:optional var silent:Bool;
}

class Notification {
	private static inline final CLASS:String = "java/androidmanager/Notifications";

	public static var enabled(get, never):Bool;

	public static function requestPermission(?callback:Bool->Void):Void {
		PermissionUtils.requestPermission("android.permission.POST_NOTIFICATIONS", function(_) {
			if (callback != null)
				callback(enabled);
		});
	}

	public static function createChannel(id:String, name:String, ?description:String, importance:NotificationImportance = DEFAULT):Void
		invoke("createChannel", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V", [id, name, description == null ? "" : description, importance]);

	public static function deleteChannel(id:String):Void
		invoke("deleteChannel", "(Ljava/lang/String;)V", [id]);

	public static function show(id:Int, title:String, message:String, ?options:NotificationOptions):Void {
		if (options == null)
			options = {};

		invoke("show", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IZZIIZZ)V", [
			id,
			title,
			message,
			options.channelId == null ? "" : options.channelId,
			options.channelName == null ? "" : options.channelName,
			options.priority == null ? 0 : options.priority,
			options.autoCancel == null ? true : options.autoCancel,
			options.ongoing == null ? false : options.ongoing,
			options.progressMax == null ? 0 : options.progressMax,
			options.progress == null ? 0 : options.progress,
			options.indeterminate == null ? false : options.indeterminate,
			options.silent == null ? false : options.silent
		]);
	}

	public static function showProgress(id:Int, title:String, message:String, progress:Int, max:Int = 100, ?channelId:String):Void {
		show(id, title, message, {
			channelId: channelId,
			ongoing: progress < max,
			autoCancel: progress >= max,
			progress: progress,
			progressMax: progress >= max ? 0 : max,
			silent: true
		});
	}

	public static function cancel(id:Int):Void
		invoke("cancel", "(I)V", [id]);

	public static function cancelAll():Void
		invoke("cancelAll", "()V", []);

	private static function get_enabled():Bool
		return invoke("areEnabled", "()Z", [], false);

	private static function invoke(method:String, signature:String, args:Array<Dynamic>, ?fallback:Dynamic):Dynamic {
		#if android
		return JNICache.call(CLASS, method, signature, args, fallback);
		#else
		return fallback;
		#end
	}
}
