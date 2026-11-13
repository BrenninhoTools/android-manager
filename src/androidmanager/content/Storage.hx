package androidmanager.content;

import androidmanager.tools.PermissionUtils;
import androidmanager.os.Build.VERSION;
#if android
import androidmanager.jni.JNICache;
#end
import haxe.Json;

enum abstract StorageLocation(Int) from Int to Int {
	var INTERNAL = 0;
	var CACHE = 1;
	var EXTERNAL = 2;
	var EXTERNAL_CACHE = 3;
	var MEDIA = 4;
	var OBB = 5;
	var ROOT = 6;
}

enum abstract PublicCollection(String) from String to String {
	var DOWNLOADS = "downloads";
	var DOCUMENTS = "documents";
	var PICTURES = "pictures";
	var MUSIC = "music";
	var MOVIES = "movies";
	var DCIM = "dcim";
}

enum abstract StorageState(String) from String to String {
	var MOUNTED = "mounted";
	var MOUNTED_READ_ONLY = "mounted_ro";
	var REMOVED = "removed";
	var UNMOUNTED = "unmounted";
	var CHECKING = "checking";
	var EJECTING = "ejecting";
	var NOFS = "nofs";
	var SHARED = "shared";
	var BAD_REMOVAL = "bad_removal";
	var UNMOUNTABLE = "unmountable";
	var UNKNOWN = "unknown";
}

typedef StorageSpace = {
	var total:Float;
	var free:Float;
	var available:Float;
	@:optional var path:String;
}

typedef StorageVolume = {
	var path:String;
	var description:String;
	var primary:Bool;
	var removable:Bool;
	var emulated:Bool;
	var state:StorageState;
	var uuid:String;
	var space:StorageSpace;
	var appDirectory:String;
}

typedef StorageEntry = {
	var name:String;
	var path:String;
	var relative:String;
	var isDirectory:Bool;
	var size:Float;
	var lastModified:Float;
	var hidden:Bool;
}

typedef PublicFile = {
	@:optional var uri:String;
	@:optional var path:String;
	@:optional var error:String;
}

typedef StorageTaskCallbacks = {
	@:optional var onProgress:(done:Int, total:Int, current:String) -> Void;
	@:optional var onComplete:(success:Bool, result:String) -> Void;
}

class Storage {
	private static inline final CLASS:String = "androidmanager/StorageTools";

	public static var state(get, never):StorageState;
	public static var available(get, never):Bool;
	public static var writable(get, never):Bool;
	public static var legacy(get, never):Bool;
	public static var volumes(get, never):Array<StorageVolume>;
	public static var primaryVolume(get, never):Null<StorageVolume>;
	public static var sdCard(get, never):Null<StorageVolume>;
	public static var hasAllFilesAccess(get, never):Bool;

	public static function getPath(location:StorageLocation, ?subfolder:String):Null<String> {
		var base:Null<String> = switch (location) {
			case INTERNAL: Context.internalFilesDir;
			case CACHE: Context.cacheDir;
			case EXTERNAL: Context.getExternalFilesDir();
			case EXTERNAL_CACHE: call("getExternalCacheDirectory", "()Ljava/lang/String;", [], null);
			case MEDIA: call("getMediaDirectory", "()Ljava/lang/String;", [], null);
			case OBB: Context.obbDir;
			case ROOT: call("getRootDirectory", "()Ljava/lang/String;", [], null);
			default: null;
		}

		if (base == null)
			return null;

		return subfolder == null || subfolder.length == 0 ? base : join(base, subfolder);
	}

	public static function getGameDirectory(?folderName:String, preferRoot:Bool = false, createNoMedia:Bool = true):String {
		var candidates:Array<Null<String>> = [];

		if (preferRoot && hasAllFilesAccess && folderName != null && folderName.length > 0)
			candidates.push(getPath(ROOT, "." + folderName));

		candidates.push(getPath(MEDIA));
		candidates.push(getPath(EXTERNAL));
		candidates.push(getPath(INTERNAL));

		for (candidate in candidates) {
			if (candidate != null && isWritable(candidate)) {
				ensureDirectory(candidate);
				if (createNoMedia && candidate.indexOf("/Android/media/") != -1)
					Storage.createNoMedia(candidate);
				return candidate;
			}
		}

		var fallback = getPath(INTERNAL);
		return fallback != null ? fallback : "";
	}

	public static function requestAccess(allFiles:Bool = false, ?callback:Bool->Void):Void {
		if (allFiles) {
			PermissionUtils.requestManageExternalStorage(callback);
			return;
		}

		var sdk = VERSION.SDK_INT;
		var permissions = if (sdk >= 33) {
			["android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO", "android.permission.READ_MEDIA_AUDIO"];
		} else if (sdk >= 30) {
			["android.permission.READ_EXTERNAL_STORAGE"];
		} else {
			["android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"];
		}

		PermissionUtils.requestPermissions(permissions, function(result) {
			if (callback != null)
				callback(result.granted.length > 0 && result.denied.length == 0);
		});
	}

	public static function getSpace(?path:String):Null<StorageSpace>
		return parse(call("getSpace", "(Ljava/lang/String;)Ljava/lang/String;", [path == null ? "" : path], null));

	public static function getState(?path:String):StorageState
		return call("getState", "(Ljava/lang/String;)Ljava/lang/String;", [path == null ? "" : path], "unknown");

	public static function isWritable(path:String):Bool
		return call("isWritable", "(Ljava/lang/String;)Z", [path], false);

	public static function hasSpaceFor(bytes:Float, ?path:String):Bool {
		var space = getSpace(path);
		return space == null || space.available < 0 || space.available >= bytes;
	}

	public static function createNoMedia(path:String):Bool
		return call("createNoMedia", "(Ljava/lang/String;)Z", [path], false);

	public static function getPublicDirectory(collection:PublicCollection):Null<String> {
		var type = switch (collection) {
			case DOCUMENTS: "Documents";
			case PICTURES: "Pictures";
			case MUSIC: "Music";
			case MOVIES: "Movies";
			case DCIM: "DCIM";
			default: "Download";
		}
		return call("getPublicDirectory", "(Ljava/lang/String;)Ljava/lang/String;", [type], null);
	}

	public static function list(path:String, recursive:Bool = false, includeHidden:Bool = false):Array<StorageEntry> {
		var result:Array<StorageEntry> = parse(call("list", "(Ljava/lang/String;ZZ)Ljava/lang/String;", [path, recursive, includeHidden], "[]"));
		return result != null ? result : [];
	}

	public static function getSize(path:String):Float
		return call("getSize", "(Ljava/lang/String;)D", [path], 0.0);

	public static function saveToPublic(source:String, collection:PublicCollection = DOWNLOADS, ?subfolder:String, ?fileName:String, ?mimeType:String):PublicFile {
		var result:PublicFile = parse(call("saveToPublic", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", [
			source,
			collection,
			subfolder == null ? "" : subfolder,
			fileName == null ? "" : fileName,
			mimeType == null ? "" : mimeType
		], null));

		return result != null ? result : {error: "Storage is not available"};
	}

	public static function saveToDownloads(source:String, ?subfolder:String, ?fileName:String):PublicFile
		return saveToPublic(source, DOWNLOADS, subfolder, fileName);

	public static function saveToDocuments(source:String, ?subfolder:String, ?fileName:String):PublicFile
		return saveToPublic(source, DOCUMENTS, subfolder, fileName);

	public static function copy(source:String, destination:String, overwrite:Bool = true, ?callbacks:StorageTaskCallbacks):Void
		task("copy", source, destination, overwrite, callbacks);

	public static function move(source:String, destination:String, overwrite:Bool = false, ?callbacks:StorageTaskCallbacks):Void
		task("move", source, destination, overwrite, callbacks);

	public static function delete(path:String, ?callbacks:StorageTaskCallbacks):Void
		task("delete", path, "", false, callbacks);

	public static function zip(source:String, zipPath:String, ?callbacks:StorageTaskCallbacks):Void
		task("zip", source, zipPath, true, callbacks);

	public static function unzip(zipPath:String, destination:String, overwrite:Bool = true, ?callbacks:StorageTaskCallbacks):Void
		task("unzip", zipPath, destination, overwrite, callbacks);

	public static function getSizeAsync(path:String, callback:Float->Void):Void {
		task("size", path, "", false, {
			onComplete: function(success, result) {
				if (callback != null)
					callback(success ? Std.parseFloat(result) : 0);
			}
		});
	}

	public static function formatBytes(bytes:Float, decimals:Int = 1):String {
		if (bytes < 0)
			return "?";

		var units = ["B", "KB", "MB", "GB", "TB"];
		var index = 0;
		while (bytes >= 1024 && index < units.length - 1) {
			bytes /= 1024;
			index++;
		}

		var factor = Math.pow(10, index == 0 ? 0 : decimals);
		return Std.string(Math.round(bytes * factor) / factor) + " " + units[index];
	}

	private static function task(operation:String, source:String, destination:String, overwrite:Bool, callbacks:StorageTaskCallbacks):Void {
		#if android
		call("runAsync", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZLorg/haxe/lime/HaxeObject;)V", [
			operation,
			source,
			destination,
			overwrite,
			new TaskHandler(callbacks)
		]);
		#else
		if (callbacks != null && callbacks.onComplete != null)
			callbacks.onComplete(false, "Storage tasks are only available on Android");
		#end
	}

	private static function ensureDirectory(path:String):Void {
		#if sys
		try {
			if (!sys.FileSystem.exists(path))
				sys.FileSystem.createDirectory(path);
		} catch (e:Dynamic) {}
		#end
	}

	private static function join(base:String, child:String):String {
		var left = StringTools.endsWith(base, "/") ? base.substr(0, base.length - 1) : base;
		var right = StringTools.startsWith(child, "/") ? child.substr(1) : child;
		return left + "/" + right;
	}

	private static function get_state():StorageState
		return getState();

	private static function get_available():Bool {
		var value = state;
		return value == MOUNTED || value == MOUNTED_READ_ONLY;
	}

	private static function get_writable():Bool
		return state == MOUNTED;

	private static function get_legacy():Bool
		return call("isLegacyStorage", "()Z", [], true);

	private static function get_volumes():Array<StorageVolume> {
		var result:Array<StorageVolume> = parse(call("getVolumes", "()Ljava/lang/String;", [], "[]"));
		return result != null ? result : [];
	}

	private static function get_primaryVolume():Null<StorageVolume> {
		for (volume in volumes)
			if (volume.primary)
				return volume;
		return null;
	}

	private static function get_sdCard():Null<StorageVolume> {
		for (volume in volumes)
			if (volume.removable && volume.state == MOUNTED)
				return volume;
		return null;
	}

	private static function get_hasAllFilesAccess():Bool
		return PermissionUtils.isExternalStorageManager();

	private static function parse(content:String):Dynamic {
		if (content == null || content.length == 0)
			return null;

		try {
			return Json.parse(content);
		} catch (e:Dynamic) {
			return null;
		}
	}

	private static function call(method:String, signature:String, args:Array<Dynamic>, ?fallback:Dynamic):Dynamic {
		#if android
		return JNICache.call(CLASS, method, signature, args, fallback);
		#else
		return fallback;
		#end
	}
}

#if android
@:noCompletion
private class TaskHandler #if (lime >= "8.0.0") implements lime.system.JNI.JNISafety #end {
	private var callbacks:StorageTaskCallbacks;

	public function new(callbacks:StorageTaskCallbacks) {
		this.callbacks = callbacks;
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onProgress(done:Int, total:Int, current:String):Void {
		if (callbacks != null && callbacks.onProgress != null)
			callbacks.onProgress(done, total, current);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onComplete(success:Bool, result:String, done:Int):Void {
		if (callbacks != null && callbacks.onComplete != null)
			callbacks.onComplete(success, result);
	}
}
#end
