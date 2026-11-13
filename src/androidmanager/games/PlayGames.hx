package androidmanager.games;

import androidmanager.callback.Callback;
#if android
import androidmanager.jni.JNICache;
#end
import haxe.Json;
import lime.app.Event;

typedef PlayGamesError = {
	var code:Int;
	var message:String;
}

typedef PlayGamesPlayer = {
	var id:String;
	var displayName:String;
	var title:String;
	var iconImageUri:String;
	var hiResImageUri:String;
	var level:Int;
	var xp:Float;
}

typedef PlayGamesAchievement = {
	var id:String;
	var name:String;
	var description:String;
	var state:AchievementState;
	var incremental:Bool;
	var currentSteps:Int;
	var totalSteps:Int;
	var xp:Float;
	var lastUpdated:Float;
}

typedef PlayGamesScore = {
	var score:Float;
	var displayScore:String;
	var rank:Float;
	var displayRank:String;
	var tag:String;
	var timestamp:Float;
}

typedef PlayGamesSnapshot = {
	var name:String;
	var title:String;
	var description:String;
	var lastModified:Float;
	var playedTime:Float;
}

enum abstract AchievementState(Int) from Int to Int {
	var UNLOCKED = 0;
	var REVEALED = 1;
	var HIDDEN = 2;
}

enum abstract LeaderboardTimeSpan(Int) from Int to Int {
	var DAILY = 0;
	var WEEKLY = 1;
	var ALL_TIME = 2;
}

enum abstract LeaderboardCollection(Int) from Int to Int {
	var PUBLIC = 0;
	var FRIENDS = 3;
}

enum abstract PopupGravity(Int) from Int to Int {
	var TOP = 49;
	var BOTTOM = 81;
	var TOP_LEFT = 51;
	var TOP_RIGHT = 53;
	var BOTTOM_LEFT = 83;
	var BOTTOM_RIGHT = 85;
}

class PlayGames {
	private static inline final CLASS:String = "androidmanager/PlayGamesManager";
	private static inline final HAXE_OBJECT:String = "Lorg/haxe/lime/HaxeObject;";
	private static inline final STRING:String = "Ljava/lang/String;";

	public static var onSignInChanged(default, null):Event<Bool->Void> = new Event<Bool->Void>();

	public static var available(get, never):Bool;
	public static var signedIn(get, never):Bool;
	public static var player(default, null):Null<PlayGamesPlayer> = null;

	private static var listening:Bool = false;

	public static function init(?onReady:Bool->Void):Void {
		#if android
		if (!available) {
			if (onReady != null)
				onReady(false);
			return;
		}

		if (!listening) {
			listening = true;
			JNICache.call(CLASS, "setListener", '($HAXE_OBJECT)V', [new AuthListener()]);
		}

		checkAuthentication(function(ok) {
			if (ok)
				refreshPlayer();
			if (onReady != null)
				onReady(ok);
		});
		#else
		if (onReady != null)
			onReady(false);
		#end
	}

	public static function checkAuthentication(?callback:Bool->Void):Void {
		request("checkAuthentication", '($HAXE_OBJECT)V', [], function(ok, data) {
			if (callback != null)
				callback(ok && data == "true");
		});
	}

	public static function signIn(?callback:(signedIn:Bool, ?error:PlayGamesError) -> Void):Void {
		request("signIn", '($HAXE_OBJECT)V', [], function(ok, data) {
			var success = ok && data == "true";
			if (success)
				refreshPlayer();
			if (callback != null)
				callback(success, ok ? null : parseError(data));
		});
	}

	public static function requestServerSideAccess(serverClientId:String, forceRefreshToken:Bool = false, callback:(code:Null<String>, ?error:PlayGamesError) -> Void):Void {
		request("requestServerSideAccess", '(${STRING}Z$HAXE_OBJECT)V', [serverClientId, forceRefreshToken], function(ok, data) {
			if (callback != null)
				callback(ok ? data : null, ok ? null : parseError(data));
		});
	}

	public static function getCurrentPlayer(callback:(player:Null<PlayGamesPlayer>, ?error:PlayGamesError) -> Void):Void {
		request("getCurrentPlayer", '($HAXE_OBJECT)V', [], function(ok, data) {
			var result:Null<PlayGamesPlayer> = ok ? parse(data) : null;
			if (result != null)
				player = result;
			if (callback != null)
				callback(result, ok ? null : parseError(data));
		});
	}

	public static function savePlayerImage(path:String, hiRes:Bool = true, callback:(path:Null<String>, ?error:PlayGamesError) -> Void):Void {
		request("savePlayerImage", '(${STRING}Z$HAXE_OBJECT)V', [path, hiRes], function(ok, data) {
			if (callback != null)
				callback(ok ? data : null, ok ? null : parseError(data));
		});
	}

	public static function unlockAchievement(id:String, ?callback:(success:Bool, ?error:PlayGamesError) -> Void):Void {
		request("unlockAchievement", '(${STRING}$HAXE_OBJECT)V', [id], simple(callback), callback != null);
	}

	public static function incrementAchievement(id:String, steps:Int = 1, ?callback:(unlocked:Bool, ?error:PlayGamesError) -> Void):Void {
		request("incrementAchievement", '(${STRING}I$HAXE_OBJECT)V', [id, steps], flag(callback), callback != null);
	}

	public static function setAchievementSteps(id:String, steps:Int, ?callback:(unlocked:Bool, ?error:PlayGamesError) -> Void):Void {
		request("setAchievementSteps", '(${STRING}I$HAXE_OBJECT)V', [id, steps], flag(callback), callback != null);
	}

	public static function revealAchievement(id:String, ?callback:(success:Bool, ?error:PlayGamesError) -> Void):Void {
		request("revealAchievement", '(${STRING}$HAXE_OBJECT)V', [id], simple(callback), callback != null);
	}

	public static function showAchievements(?onClose:Void->Void):Void {
		var requestCode = waitClose(onClose);
		request("showAchievements", '(I$HAXE_OBJECT)V', [requestCode], function(ok, _) {
			if (!ok)
				closeFailed(requestCode, onClose);
		});
	}

	public static function loadAchievements(forceReload:Bool = false, callback:(achievements:Array<PlayGamesAchievement>, ?error:PlayGamesError) -> Void):Void {
		request("loadAchievements", '(Z$HAXE_OBJECT)V', [forceReload], function(ok, data) {
			var list:Array<PlayGamesAchievement> = ok ? parse(data) : null;
			if (callback != null)
				callback(list != null ? list : [], ok ? null : parseError(data));
		});
	}

	public static function submitScore(leaderboardId:String, score:Float, ?tag:String, ?callback:(success:Bool, ?error:PlayGamesError) -> Void):Void {
		request("submitScore", '(${STRING}D${STRING}$HAXE_OBJECT)V', [leaderboardId, score, tag == null ? "" : tag], simple(callback), callback != null);
	}

	public static function showLeaderboard(leaderboardId:String, timeSpan:LeaderboardTimeSpan = ALL_TIME, collection:LeaderboardCollection = PUBLIC,
			?onClose:Void->Void):Void {
		var requestCode = waitClose(onClose);
		request("showLeaderboard", '(${STRING}III$HAXE_OBJECT)V', [leaderboardId, timeSpan, collection, requestCode], function(ok, _) {
			if (!ok)
				closeFailed(requestCode, onClose);
		});
	}

	public static function showAllLeaderboards(?onClose:Void->Void):Void {
		showLeaderboard("", ALL_TIME, PUBLIC, onClose);
	}

	public static function loadPlayerScore(leaderboardId:String, timeSpan:LeaderboardTimeSpan = ALL_TIME, collection:LeaderboardCollection = PUBLIC,
			callback:(score:Null<PlayGamesScore>, ?error:PlayGamesError) -> Void):Void {
		request("loadPlayerScore", '(${STRING}II$HAXE_OBJECT)V', [leaderboardId, timeSpan, collection], function(ok, data) {
			if (callback != null)
				callback(ok ? parse(data) : null, ok ? null : parseError(data));
		});
	}

	public static function incrementEvent(eventId:String, amount:Int = 1):Void {
		#if android
		JNICache.call(CLASS, "incrementEvent", '(${STRING}I)V', [eventId, amount]);
		#end
	}

	public static function setPopupGravity(gravity:PopupGravity = TOP):Void {
		#if android
		JNICache.call(CLASS, "setPopupGravity", "(I)V", [gravity]);
		#end
	}

	public static function saveGame(name:String, data:String, ?description:String, playedTimeMillis:Float = -1,
			?callback:(snapshot:Null<PlayGamesSnapshot>, ?error:PlayGamesError) -> Void):Void {
		request("saveGame", '(${STRING}${STRING}${STRING}D$HAXE_OBJECT)V', [name, data, description == null ? "" : description, playedTimeMillis],
			function(ok, result) {
				if (callback != null)
					callback(ok ? parse(result) : null, ok ? null : parseError(result));
			});
	}

	public static function loadGame(name:String, callback:(data:Null<String>, ?error:PlayGamesError) -> Void):Void {
		request("loadGame", '(${STRING}$HAXE_OBJECT)V', [name], function(ok, data) {
			if (callback != null)
				callback(ok ? data : null, ok ? null : parseError(data));
		});
	}

	public static function deleteGame(name:String, ?callback:(success:Bool, ?error:PlayGamesError) -> Void):Void {
		request("deleteGame", '(${STRING}$HAXE_OBJECT)V', [name], simple(callback));
	}

	public static function listSavedGames(forceReload:Bool = false, callback:(snapshots:Array<PlayGamesSnapshot>, ?error:PlayGamesError) -> Void):Void {
		request("listSavedGames", '(Z$HAXE_OBJECT)V', [forceReload], function(ok, data) {
			var list:Array<PlayGamesSnapshot> = ok ? parse(data) : null;
			if (callback != null)
				callback(list != null ? list : [], ok ? null : parseError(data));
		});
	}

	public static function showSavedGames(title:String = "Saved Games", allowAdd:Bool = false, allowDelete:Bool = true, maxSnapshots:Int = 5,
			?onClose:ActivityResult->Void):Void {
		var requestCode = Callback.nextRequestCode();
		if (onClose != null)
			Callback.waitForActivityResult(requestCode, onClose);

		request("showSavedGames", '(${STRING}ZZII$HAXE_OBJECT)V', [title, allowAdd, allowDelete, maxSnapshots, requestCode], function(ok, _) {
			if (!ok)
				Callback.cancelWait(requestCode);
		});
	}

	public static function refreshPlayer():Void {
		getCurrentPlayer(function(p, ?e) {});
	}

	@:noCompletion
	public static function __authChanged(value:Bool):Void {
		if (!value)
			player = null;
		onSignInChanged.dispatch(value);
	}

	private static function get_available():Bool {
		#if android
		return JNICache.getStaticMethod(CLASS, "isInitialized", "()Z") != null;
		#else
		return false;
		#end
	}

	private static function get_signedIn():Bool {
		#if android
		return JNICache.call(CLASS, "isSignedIn", "()Z", [], false);
		#else
		return false;
		#end
	}

	private static function request(method:String, signature:String, args:Array<Dynamic>, handler:(Bool, String) -> Void, withCallback:Bool = true):Void {
		#if android
		if (!available) {
			if (handler != null)
				handler(false, '{"code":-1,"message":"Play Games is not enabled, define PLAY_GAMES_APP_ID"}');
			return;
		}

		var callArgs = args.copy();
		callArgs.push(withCallback ? new ResultHandler(handler) : null);
		JNICache.call(CLASS, method, signature, callArgs);
		#else
		if (handler != null)
			handler(false, '{"code":-1,"message":"Play Games is only available on Android"}');
		#end
	}

	private static function simple(callback:(Bool, ?PlayGamesError) -> Void):(Bool, String) -> Void {
		return function(ok, data) {
			if (callback != null)
				callback(ok, ok ? null : parseError(data));
		};
	}

	private static function flag(callback:(Bool, ?PlayGamesError) -> Void):(Bool, String) -> Void {
		return function(ok, data) {
			if (callback != null)
				callback(ok && data == "true", ok ? null : parseError(data));
		};
	}

	private static function waitClose(onClose:Void->Void):Int {
		var requestCode = Callback.nextRequestCode();
		if (onClose != null)
			Callback.waitForActivityResult(requestCode, function(_) onClose());
		return requestCode;
	}

	private static function closeFailed(requestCode:Int, onClose:Void->Void):Void {
		Callback.cancelWait(requestCode);
		if (onClose != null)
			onClose();
	}

	private static function parse(content:String):Dynamic {
		if (content == null || content.length == 0 || content == "null")
			return null;

		try {
			return Json.parse(content);
		} catch (e:Dynamic) {
			return null;
		}
	}

	private static function parseError(content:String):PlayGamesError {
		var result:Dynamic = parse(content);
		if (result != null && Reflect.hasField(result, "message"))
			return result;

		return {code: -1, message: content != null ? content : "Unknown error"};
	}
}

#if android
@:noCompletion
private class ResultHandler #if (lime >= "8.0.0") implements lime.system.JNI.JNISafety #end {
	private var handler:(Bool, String) -> Void;

	public function new(handler:(Bool, String) -> Void) {
		this.handler = handler;
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onResult(success:Bool, data:String):Void {
		if (handler != null)
			handler(success, data);
	}
}

@:noCompletion
private class AuthListener #if (lime >= "8.0.0") implements lime.system.JNI.JNISafety #end {
	public function new() {}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onAuthChanged(value:Bool):Void {
		PlayGames.__authChanged(value);
	}
}
#end
