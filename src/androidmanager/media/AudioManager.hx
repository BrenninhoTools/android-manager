package androidmanager.media;

#if android
import androidmanager.jni.JNICache;
#end
import haxe.Json;
import lime.app.Event;

enum abstract AudioStream(Int) from Int to Int {
	var VOICE_CALL = 0;
	var SYSTEM = 1;
	var RING = 2;
	var MUSIC = 3;
	var ALARM = 4;
	var NOTIFICATION = 5;
	var ACCESSIBILITY = 10;
}

enum abstract AudioDirection(Int) from Int to Int {
	var LOWER = -1;
	var SAME = 0;
	var RAISE = 1;
	var MUTE = -100;
	var UNMUTE = 100;
	var TOGGLE_MUTE = 101;
}

enum abstract AudioFlag(Int) from Int to Int {
	var NONE = 0;
	var SHOW_UI = 1;
	var ALLOW_RINGER_MODES = 2;
	var PLAY_SOUND = 4;
	var REMOVE_SOUND_AND_VIBRATE = 8;
	var VIBRATE = 16;
}

enum abstract AudioFocus(Int) from Int to Int {
	var GAIN = 1;
	var GAIN_TRANSIENT = 2;
	var GAIN_TRANSIENT_MAY_DUCK = 3;
	var GAIN_TRANSIENT_EXCLUSIVE = 4;
	var LOSS = -1;
	var LOSS_TRANSIENT = -2;
	var LOSS_TRANSIENT_CAN_DUCK = -3;
}

enum abstract AudioFocusResult(Int) from Int to Int {
	var FAILED = 0;
	var GRANTED = 1;
	var DELAYED = 2;
}

enum abstract RingerMode(Int) from Int to Int {
	var UNKNOWN = -1;
	var SILENT = 0;
	var VIBRATE = 1;
	var NORMAL = 2;
}

typedef AudioDevice = {
	var id:Int;
	var type:Int;
	var name:String;
}

class AudioManager {
	private static inline final CLASS:String = "java/androidmanager/Audio";

	public static var onFocusChange(default, null):Event<AudioFocus->Void> = new Event<AudioFocus->Void>();

	public static var musicActive(get, never):Bool;
	public static var headphonesConnected(get, never):Bool;
	public static var ringerMode(get, never):RingerMode;
	public static var outputSampleRate(get, never):Int;
	public static var outputFramesPerBuffer(get, never):Int;

	public static function getStreamVolume(stream:AudioStream = MUSIC):Int
		return invoke("getStreamVolume", "(I)I", [stream], -1);

	public static function getStreamMaxVolume(stream:AudioStream = MUSIC):Int
		return invoke("getStreamMaxVolume", "(I)I", [stream], -1);

	public static function getStreamMinVolume(stream:AudioStream = MUSIC):Int
		return invoke("getStreamMinVolume", "(I)I", [stream], -1);

	public static function getStreamVolumePercent(stream:AudioStream = MUSIC):Float {
		var max = getStreamMaxVolume(stream);
		return max > 0 ? getStreamVolume(stream) / max : 0;
	}

	public static function setStreamVolume(volume:Int, stream:AudioStream = MUSIC, flags:AudioFlag = NONE):Void
		invoke("setStreamVolume", "(III)V", [stream, volume, flags]);

	public static function setStreamVolumePercent(percent:Float, stream:AudioStream = MUSIC, flags:AudioFlag = NONE):Void {
		var max = getStreamMaxVolume(stream);
		if (max > 0)
			setStreamVolume(Math.round(Math.max(0, Math.min(1, percent)) * max), stream, flags);
	}

	public static function adjustStreamVolume(direction:AudioDirection, stream:AudioStream = MUSIC, flags:AudioFlag = NONE):Void
		invoke("adjustStreamVolume", "(III)V", [stream, direction, flags]);

	public static function isStreamMute(stream:AudioStream = MUSIC):Bool
		return invoke("isStreamMute", "(I)Z", [stream], false);

	public static function getOutputDevices():Array<AudioDevice> {
		try {
			var result:Array<AudioDevice> = Json.parse(invoke("getOutputDevices", "()Ljava/lang/String;", [], "[]"));
			return result != null ? result : [];
		} catch (e:Dynamic) {
			return [];
		}
	}

	public static function requestAudioFocus(focus:AudioFocus = GAIN, stream:AudioStream = MUSIC):AudioFocusResult {
		#if android
		return JNICache.call(CLASS, "requestAudioFocus", "(Lorg/haxe/lime/HaxeObject;II)I", [new FocusHandler(), stream, focus], 0);
		#else
		return FAILED;
		#end
	}

	public static function abandonAudioFocus():AudioFocusResult
		return invoke("abandonAudioFocus", "()I", [], 0);

	private static function get_musicActive():Bool
		return invoke("isMusicActive", "()Z", [], false);

	private static function get_headphonesConnected():Bool
		return invoke("isHeadphonesConnected", "()Z", [], false);

	private static function get_ringerMode():RingerMode
		return invoke("getRingerMode", "()I", [], -1);

	private static function get_outputSampleRate():Int
		return invoke("getOutputSampleRate", "()I", [], -1);

	private static function get_outputFramesPerBuffer():Int
		return invoke("getOutputFramesPerBuffer", "()I", [], -1);

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
private class FocusHandler #if (lime >= "8.0.0") implements lime.system.JNI.JNISafety #end {
	public function new() {}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onFocusChange(change:Int):Void {
		AudioManager.onFocusChange.dispatch(change);
	}
}
#end
