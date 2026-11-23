package androidmanager.content;

#if android
import androidmanager.jni.JNICache;
#end

typedef AssetCopyJob = {
	var source:String;
	var target:String;
	@:optional var fromFile:Bool;
	@:optional var id:Dynamic;
}

typedef AssetBatchProgress = {
	var completed:Int;
	var total:Int;
	var bytes:Float;
	var elapsed:Float;
	var running:Bool;
}

class Assets {
	private static inline var CLASS:String = "androidmanager/AssetTools";

	private static var batchJobs:Map<String, AssetCopyJob> = new Map();

	public static var batchRunning(get, never):Bool;

	public static var batchCompleted(get, never):Int;

	public static var batchTotal(get, never):Int;

	public static function exists(asset:String):Bool
		return call("exists", "(Ljava/lang/String;)Z", [asset], false);

	public static function list(folder:String = ""):Array<String> {
		var content:String = call("list", "(Ljava/lang/String;)Ljava/lang/String;", [folder], "");
		return content == null || content.length == 0 ? [] : content.split("\n");
	}

	public static function readText(asset:String):Null<String>
		return call("readText", "(Ljava/lang/String;)Ljava/lang/String;", [asset], null);

	public static function size(asset:String):Float
		return call("size", "(Ljava/lang/String;)D", [asset], -1);

	public static function copy(asset:String, target:String):Bool
		return call("copy", "(Ljava/lang/String;Ljava/lang/String;)Z", [asset, target], false);

	public static function copyFolder(folder:String, target:String, overwrite:Bool = true):Int
		return call("copyFolder", "(Ljava/lang/String;Ljava/lang/String;Z)I", [folder, target, overwrite], 0);

	public static function batch(jobs:Array<AssetCopyJob>, threads:Int = 4):Bool {
		if (jobs == null || batchRunning)
			return false;

		batchJobs.clear();
		var lines:Array<String> = [];
		for (job in jobs) {
			if (job == null || job.source == null || job.target == null)
				continue;
			if (job.source.indexOf("\t") >= 0 || job.source.indexOf("\n") >= 0 || job.target.indexOf("\t") >= 0 || job.target.indexOf("\n") >= 0)
				continue;
			var line = (job.fromFile == true ? "F" : "A") + "\t" + job.source + "\t" + job.target;
			batchJobs.set(line, job);
			lines.push(line);
		}

		var started:Bool = call("startBatch", "(Ljava/lang/String;I)Z", [lines.join("\n"), threads], false);
		if (!started)
			batchJobs.clear();
		return started;
	}

	public static function progress():AssetBatchProgress {
		return {
			completed: batchCompleted,
			total: batchTotal,
			bytes: call("getBatchBytes", "()D", [], 0),
			elapsed: call("getBatchElapsed", "()D", [], 0),
			running: batchRunning
		};
	}

	public static function getFailed():Array<AssetCopyJob> {
		var content:String = call("getBatchFailed", "()Ljava/lang/String;", [], "");
		var failed:Array<AssetCopyJob> = [];
		if (content == null || content.length == 0)
			return failed;
		for (line in content.split("\n")) {
			var job = batchJobs.get(line);
			if (job != null)
				failed.push(job);
		}
		return failed;
	}

	public static function cancel():Void
		call("cancelBatch", "()V", []);

	private static function get_batchRunning():Bool
		return call("isBatchRunning", "()Z", [], false);

	private static function get_batchCompleted():Int
		return call("getBatchCompleted", "()I", [], 0);

	private static function get_batchTotal():Int
		return call("getBatchTotal", "()I", [], 0);

	private static function call(method:String, signature:String, args:Array<Dynamic>, ?fallback:Dynamic):Dynamic {
		#if android
		return JNICache.call(CLASS, method, signature, args, fallback);
		#else
		return fallback;
		#end
	}
}
