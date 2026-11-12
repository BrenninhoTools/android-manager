package androidmanager.net;

#if android
import androidmanager.jni.JNICache;
#end
import haxe.Json;
import lime.app.Event;

enum abstract ConnectionType(String) from String to String {
	var NONE = "none";
	var WIFI = "wifi";
	var CELLULAR = "cellular";
	var ETHERNET = "ethernet";
	var BLUETOOTH = "bluetooth";
	var VPN = "vpn";
	var OTHER = "other";
}

typedef NetworkStatus = {
	var connected:Bool;
	var type:ConnectionType;
	var metered:Bool;
}

class Network {
	private static inline final CLASS:String = "androidmanager/Network";

	public static var onChange(default, null):Event<NetworkStatus->Void> = new Event<NetworkStatus->Void>();

	public static var connected(get, never):Bool;
	public static var connectionType(get, never):ConnectionType;
	public static var metered(get, never):Bool;
	public static var status(get, never):NetworkStatus;
	public static var downstreamKbps(get, never):Int;
	public static var upstreamKbps(get, never):Int;
	public static var listening(default, null):Bool = false;

	public static function startListening():Void {
		if (listening)
			return;

		listening = true;
		#if android
		JNICache.call(CLASS, "startListening", "(Lorg/haxe/lime/HaxeObject;)V", [new NetworkHandler()]);
		#end
	}

	public static function stopListening():Void {
		if (!listening)
			return;

		listening = false;
		invoke("stopListening", "()V", []);
	}

	@:noCompletion
	public static function __parse(content:String):NetworkStatus {
		try {
			var result:NetworkStatus = Json.parse(content);
			if (result != null)
				return result;
		} catch (e:Dynamic) {}

		return {connected: false, type: NONE, metered: false};
	}

	private static function get_connected():Bool
		return invoke("isConnected", "()Z", [], false);

	private static function get_connectionType():ConnectionType
		return invoke("getConnectionType", "()Ljava/lang/String;", [], "none");

	private static function get_metered():Bool
		return invoke("isMetered", "()Z", [], false);

	private static function get_status():NetworkStatus
		return __parse(invoke("getStatus", "()Ljava/lang/String;", [], "{}"));

	private static function get_downstreamKbps():Int
		return invoke("getDownstreamKbps", "()I", [], -1);

	private static function get_upstreamKbps():Int
		return invoke("getUpstreamKbps", "()I", [], -1);

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
private class NetworkHandler #if (lime >= "8.0.0") implements lime.system.JNI.JNISafety #end {
	public function new() {}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onNetworkChanged(content:String):Void {
		Network.onChange.dispatch(Network.__parse(content));
	}
}
#end
