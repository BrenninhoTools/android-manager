package androidmanager.content;

import androidmanager.callback.Callback;
#if android
import androidmanager.jni.JNICache;
#end
import haxe.Json;

typedef UriInfo = {
	var uri:String;
	var mimeType:String;
	var name:String;
	var size:Float;
	var lastModified:Float;
}

typedef DocumentEntry = {
	var name:String;
	var uri:String;
	var mimeType:String;
	var isDirectory:Bool;
	var size:Float;
	var lastModified:Float;
}

class FileManager {
	private static inline final CLASS:String = "androidmanager/FileManager";

	public static function pickFile(callback:Array<String>->Void, ?mimeTypes:Array<String>, multiple:Bool = false):Void {
		var requestCode = Callback.nextRequestCode();
		Callback.waitForActivityResult(requestCode, function(result) {
			if (callback != null)
				callback(urisFrom(result));
		});

		var types = mimeTypes == null || mimeTypes.length == 0 ? "*/*" : mimeTypes.join(",");
		if (!invoke("pickFile", "(ILjava/lang/String;Z)Z", [requestCode, types, multiple], false))
			fail(requestCode, callback);
	}

	public static function pickDirectory(callback:Null<String>->Void, persist:Bool = true, ?initialUri:String):Void {
		var requestCode = Callback.nextRequestCode();
		Callback.waitForActivityResult(requestCode, function(result) {
			var uris = urisFrom(result);
			var uri = uris.length > 0 ? uris[0] : null;
			if (uri != null && persist)
				takePersistablePermission(uri);
			if (callback != null)
				callback(uri);
		});

		if (!invoke("pickDirectory", "(ILjava/lang/String;)Z", [requestCode, initialUri == null ? "" : initialUri], false)) {
			Callback.cancelWait(requestCode);
			if (callback != null)
				callback(null);
		}
	}

	public static function createDocument(fileName:String, callback:Null<String>->Void, mimeType:String = "application/octet-stream"):Void {
		var requestCode = Callback.nextRequestCode();
		Callback.waitForActivityResult(requestCode, function(result) {
			var uris = urisFrom(result);
			if (callback != null)
				callback(uris.length > 0 ? uris[0] : null);
		});

		if (!invoke("createDocument", "(ILjava/lang/String;Ljava/lang/String;)Z", [requestCode, fileName, mimeType], false)) {
			Callback.cancelWait(requestCode);
			if (callback != null)
				callback(null);
		}
	}

	public static function exportFile(path:String, fileName:String, ?callback:Bool->Void, mimeType:String = "application/octet-stream"):Void {
		createDocument(fileName, function(uri) {
			var ok = uri != null && copyFileToUri(path, uri);
			if (callback != null)
				callback(ok);
		}, mimeType);
	}

	public static function importFile(destination:String, ?callback:Null<String>->Void, ?mimeTypes:Array<String>):Void {
		pickFile(function(uris) {
			var ok = uris.length > 0 && copyUriToFile(uris[0], destination);
			if (callback != null)
				callback(ok ? destination : null);
		}, mimeTypes, false);
	}

	public static function takePersistablePermission(uri:String):Bool
		return invoke("takePersistablePermission", "(Ljava/lang/String;)Z", [uri], false);

	public static function releasePersistablePermission(uri:String):Void
		invoke("releasePersistablePermission", "(Ljava/lang/String;)V", [uri]);

	public static function getPersistedUris():Array<String>
		return parseArray(invoke("getPersistedUris", "()Ljava/lang/String;", [], "[]"));

	public static function getUriInfo(uri:String):Null<UriInfo> {
		var content:String = invoke("getUriInfo", "(Ljava/lang/String;)Ljava/lang/String;", [uri], null);
		if (content == null)
			return null;

		try {
			return Json.parse(content);
		} catch (e:Dynamic) {
			return null;
		}
	}

	public static function copyUriToFile(uri:String, path:String):Bool
		return invoke("copyUriToFile", "(Ljava/lang/String;Ljava/lang/String;)Z", [uri, path], false);

	public static function copyFileToUri(path:String, uri:String):Bool
		return invoke("copyFileToUri", "(Ljava/lang/String;Ljava/lang/String;)Z", [path, uri], false);

	public static function readUri(uri:String):Null<String>
		return invoke("readUriAsString", "(Ljava/lang/String;)Ljava/lang/String;", [uri], null);

	public static function writeUri(uri:String, content:String):Bool
		return invoke("writeStringToUri", "(Ljava/lang/String;Ljava/lang/String;)Z", [uri, content], false);

	public static function listDirectory(treeUri:String):Array<DocumentEntry>
		return parseArray(invoke("listDirectory", "(Ljava/lang/String;)Ljava/lang/String;", [treeUri], "[]"));

	public static function copyDirectory(treeUri:String, path:String):Int
		return invoke("copyTreeToPath", "(Ljava/lang/String;Ljava/lang/String;)I", [treeUri, path], -1);

	public static function copyDirectoryAsync(treeUri:String, path:String, ?onComplete:(count:Int, error:String) -> Void, ?onProgress:(count:Int, file:String) -> Void):Void {
		#if android
		JNICache.call(CLASS, "copyTreeToPathAsync", "(Ljava/lang/String;Ljava/lang/String;Lorg/haxe/lime/HaxeObject;)V", [treeUri, path, new CopyHandler(onComplete, onProgress)]);
		#else
		if (onComplete != null)
			onComplete(-1, "Unsupported platform");
		#end
	}

	public static function deleteDocument(uri:String):Bool
		return invoke("deleteDocument", "(Ljava/lang/String;)Z", [uri], false);

	public static function openUri(uri:String, ?mimeType:String):Bool
		return invoke("openUri", "(Ljava/lang/String;Ljava/lang/String;)Z", [uri, mimeType == null ? "" : mimeType], false);

	public static function scanMedia(path:String):Void
		invoke("scanMedia", "(Ljava/lang/String;)V", [path]);

	private static function urisFrom(result:ActivityResult):Array<String> {
		if (result == null || result.resultCode != ResultCode.OK || result.data == null)
			return [];

		var uris = result.data.uris;
		if (uris != null && uris.length > 0)
			return uris;

		return result.data.data != null ? [result.data.data] : [];
	}

	private static function fail(requestCode:Int, callback:Array<String>->Void):Void {
		Callback.cancelWait(requestCode);
		if (callback != null)
			callback([]);
	}

	private static function parseArray(content:String):Dynamic {
		try {
			var result:Array<Dynamic> = Json.parse(content);
			return result != null ? result : [];
		} catch (e:Dynamic) {
			return [];
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
private class CopyHandler #if (lime >= "8.0.0") implements lime.system.JNI.JNISafety #end {
	private var complete:(Int, String) -> Void;
	private var progress:(Int, String) -> Void;

	public function new(complete:(Int, String) -> Void, progress:(Int, String) -> Void) {
		this.complete = complete;
		this.progress = progress;
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onProgress(count:Int, file:String):Void {
		if (progress != null)
			progress(count, file);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onComplete(count:Int, error:String):Void {
		if (complete != null)
			complete(count, error);
	}
}
#end
