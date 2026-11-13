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

enum abstract AudioMode(Int) from Int to Int {
	var NORMAL = 0;
	var RINGTONE = 1;
	var IN_CALL = 2;
	var IN_COMMUNICATION = 3;
	var CALL_SCREENING = 4;
}

enum abstract InterruptionFilter(Int) from Int to Int {
	var UNKNOWN = 0;
	var ALL = 1;
	var PRIORITY = 2;
	var NONE = 3;
	var ALARMS = 4;
}

enum abstract SoundEffect(Int) from Int to Int {
	var KEY_CLICK = 0;
	var FOCUS_NAVIGATION_UP = 1;
	var FOCUS_NAVIGATION_DOWN = 2;
	var FOCUS_NAVIGATION_LEFT = 3;
	var FOCUS_NAVIGATION_RIGHT = 4;
	var KEYPRESS_STANDARD = 5;
	var KEYPRESS_SPACEBAR = 6;
	var KEYPRESS_DELETE = 7;
	var KEYPRESS_RETURN = 8;
	var KEYPRESS_INVALID = 9;
}

typedef AudioDevice = {
	var id:Int;
	var type:Int;
	var name:String;
	@:optional var typeName:String;
	@:optional var sink:Bool;
	@:optional var source:Bool;
	@:optional var headphones:Bool;
	@:optional var bluetooth:Bool;
	@:optional var address:String;
	@:optional var sampleRates:Array<Int>;
	@:optional var channelCounts:Array<Int>;
	@:optional var encodings:Array<Int>;
}

typedef StreamVolume = {
	var volume:Int;
	var max:Int;
	var min:Int;
	var muted:Bool;
}

typedef AudioInfo = {
	var sampleRate:Int;
	var framesPerBuffer:Int;
	var outputLatency:Int;
	var lowLatency:Bool;
	var proAudio:Bool;
	var musicActive:Bool;
	var mode:AudioMode;
	var ringerMode:RingerMode;
	var callActive:Bool;
	var headphones:Bool;
	var bluetooth:Bool;
	var microphoneMute:Bool;
	var interruptionFilter:InterruptionFilter;
	var spatializerAvailable:Bool;
	var spatializerEnabled:Bool;
	var headTrackerAvailable:Bool;
	var volumeControlStream:Int;
	var volumes:Dynamic<StreamVolume>;
}

class AudioManager {
	private static inline final CLASS:String = "androidmanager/Audio";

	public static var onFocusChange(default, null):Event<AudioFocus->Void> = new Event<AudioFocus->Void>();
	public static var onShouldPause(default, null):Event<Void->Void> = new Event<Void->Void>();
	public static var onShouldResume(default, null):Event<Void->Void> = new Event<Void->Void>();
	public static var onDuck(default, null):Event<Bool->Void> = new Event<Bool->Void>();
	public static var onVolumeChange(default, null):Event<(AudioStream, Int, Int) -> Void> = new Event<(AudioStream, Int, Int) -> Void>();
	public static var onBecomingNoisy(default, null):Event<Void->Void> = new Event<Void->Void>();
	public static var onRingerModeChange(default, null):Event<RingerMode->Void> = new Event<RingerMode->Void>();
	public static var onModeChange(default, null):Event<AudioMode->Void> = new Event<AudioMode->Void>();
	public static var onHeadphonesChange(default, null):Event<Bool->Void> = new Event<Bool->Void>();
	public static var onDeviceConnected(default, null):Event<AudioDevice->Void> = new Event<AudioDevice->Void>();
	public static var onDeviceDisconnected(default, null):Event<AudioDevice->Void> = new Event<AudioDevice->Void>();

	public static var listening(default, null):Bool = false;
	public static var focused(default, null):Bool = false;
	public static var ducked(default, null):Bool = false;
	public static var pausedByFocus(default, null):Bool = false;
	public static var duckVolume:Float = 0.3;

	public static var bluetoothConnected(get, never):Bool;
	public static var callActive(get, never):Bool;
	public static var mode(get, never):AudioMode;
	public static var interruptionFilter(get, never):InterruptionFilter;
	public static var lowLatency(get, never):Bool;
	public static var proAudio(get, never):Bool;
	public static var outputLatency(get, never):Int;
	public static var microphoneMute(get, never):Bool;
	public static var spatializerAvailable(get, never):Bool;
	public static var spatializerEnabled(get, never):Bool;
	public static var info(get, never):Null<AudioInfo>;
	public static var volumeControlStream(get, set):AudioStream;

	private static var autoPause:Void->Void = null;
	private static var autoResume:Void->Void = null;
	private static var autoVolume:Float->Void = null;
	private static var autoHandled:Bool = false;

	public static function startListening():Void {
		if (listening)
			return;

		listening = true;
		#if android
		JNICache.call(CLASS, "startListening", "(Lorg/haxe/lime/HaxeObject;)V", [new EventHandler()]);
		#end
	}

	public static function stopListening():Void {
		if (!listening)
			return;

		listening = false;
		invoke("stopListening", "()V", []);
	}

	public static function autoHandle(pause:Void->Void, resume:Void->Void, ?setVolume:Float->Void, pauseOnCall:Bool = true, pauseOnNoisy:Bool = true):Void {
		autoPause = pause;
		autoResume = resume;
		autoVolume = setVolume;

		if (!autoHandled) {
			autoHandled = true;
			onShouldPause.add(function() if (autoPause != null) autoPause());
			onShouldResume.add(function() if (autoResume != null) autoResume());
			onDuck.add(function(value) if (autoVolume != null) autoVolume(value ? duckVolume : 1));
			onBecomingNoisy.add(function() if (pauseOnNoisy && autoPause != null) autoPause());
			onModeChange.add(function(value) {
				if (!pauseOnCall)
					return;

				var inCall = value == IN_CALL || value == IN_COMMUNICATION || value == RINGTONE;
				if (inCall && autoPause != null)
					autoPause();
				else if (!inCall && !pausedByFocus && autoResume != null)
					autoResume();
			});
		}

		volumeControlStream = MUSIC;
		requestAudioFocus(GAIN, MUSIC);
		startListening();
	}

	public static function getDevices(outputs:Bool = true):Array<AudioDevice> {
		try {
			var result:Array<AudioDevice> = Json.parse(invoke("getDevices", "(Z)Ljava/lang/String;", [outputs], "[]"));
			return result != null ? result : [];
		} catch (e:Dynamic) {
			return [];
		}
	}

	public static function setStreamMute(mute:Bool, stream:AudioStream = MUSIC, flags:AudioFlag = NONE):Void
		invoke("setStreamMute", "(IZI)V", [stream, mute, flags]);

	public static function playSoundEffect(effect:SoundEffect = KEY_CLICK, volume:Float = -1):Void
		invoke("playSoundEffect", "(IF)V", [effect, volume]);

	public static function getStreamInfo(stream:AudioStream = MUSIC):StreamVolume {
		return {
			volume: getStreamVolume(stream),
			max: getStreamMaxVolume(stream),
			min: getStreamMinVolume(stream),
			muted: isStreamMute(stream)
		};
	}

	@:noCompletion
	public static function __focus(change:AudioFocus):Void {
		onFocusChange.dispatch(change);

		switch (change) {
			case GAIN | GAIN_TRANSIENT | GAIN_TRANSIENT_MAY_DUCK | GAIN_TRANSIENT_EXCLUSIVE:
				focused = true;
				if (ducked) {
					ducked = false;
					onDuck.dispatch(false);
				}
				if (pausedByFocus) {
					pausedByFocus = false;
					onShouldResume.dispatch();
				}
			case LOSS_TRANSIENT_CAN_DUCK:
				if (!ducked) {
					ducked = true;
					onDuck.dispatch(true);
				}
			case LOSS_TRANSIENT:
				focused = false;
				if (!pausedByFocus) {
					pausedByFocus = true;
					onShouldPause.dispatch();
				}
			case LOSS:
				focused = false;
				pausedByFocus = false;
				onShouldPause.dispatch();
			default:
		}
	}

	@:noCompletion
	public static function __device(added:Bool, content:String):Void {
		var device:AudioDevice = null;
		try {
			device = Json.parse(content);
		} catch (e:Dynamic) {}

		if (device == null)
			return;

		if (added)
			onDeviceConnected.dispatch(device);
		else
			onDeviceDisconnected.dispatch(device);
	}

	private static function get_bluetoothConnected():Bool
		return invoke("isBluetoothConnected", "()Z", [], false);

	private static function get_callActive():Bool
		return invoke("isCallActive", "()Z", [], false);

	private static function get_mode():AudioMode
		return invoke("getMode", "()I", [], 0);

	private static function get_interruptionFilter():InterruptionFilter
		return invoke("getInterruptionFilter", "()I", [], 1);

	private static function get_lowLatency():Bool
		return invoke("hasLowLatency", "()Z", [], false);

	private static function get_proAudio():Bool
		return invoke("hasProAudio", "()Z", [], false);

	private static function get_outputLatency():Int
		return invoke("getOutputLatency", "(I)I", [MUSIC], -1);

	private static function get_microphoneMute():Bool
		return invoke("isMicrophoneMute", "()Z", [], false);

	private static function get_spatializerAvailable():Bool
		return invoke("isSpatializerAvailable", "()Z", [], false);

	private static function get_spatializerEnabled():Bool
		return invoke("isSpatializerEnabled", "()Z", [], false);

	private static function get_info():Null<AudioInfo> {
		try {
			return Json.parse(invoke("getInfo", "()Ljava/lang/String;", [], "null"));
		} catch (e:Dynamic) {
			return null;
		}
	}

	private static function get_volumeControlStream():AudioStream
		return invoke("getVolumeControlStream", "()I", [], -1);

	private static function set_volumeControlStream(value:AudioStream):AudioStream {
		invoke("setVolumeControlStream", "(I)V", [value]);
		return value;
	}

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

	public static function getOutputDevices():Array<AudioDevice>
		return getDevices(true);

	public static function getInputDevices():Array<AudioDevice>
		return getDevices(false);

	public static function requestAudioFocus(focus:AudioFocus = GAIN, stream:AudioStream = MUSIC):AudioFocusResult {
		#if android
		var result:AudioFocusResult = JNICache.call(CLASS, "requestAudioFocus", "(Lorg/haxe/lime/HaxeObject;II)I", [new FocusHandler(), stream, focus], 0);
		if (result == GRANTED) {
			focused = true;
			pausedByFocus = false;
		}
		return result;
		#else
		return FAILED;
		#end
	}

	public static function abandonAudioFocus():AudioFocusResult {
		focused = false;
		pausedByFocus = false;
		return invoke("abandonAudioFocus", "()I", [], 0);
	}

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
		AudioManager.__focus(change);
	}
}

@:noCompletion
private class EventHandler #if (lime >= "8.0.0") implements lime.system.JNI.JNISafety #end {
	public function new() {}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onVolumeChanged(stream:Int, volume:Int, max:Int):Void {
		AudioManager.onVolumeChange.dispatch(stream, volume, max);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onBecomingNoisy():Void {
		AudioManager.onBecomingNoisy.dispatch();
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onRingerModeChanged(mode:Int):Void {
		AudioManager.onRingerModeChange.dispatch(mode);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onModeChanged(mode:Int):Void {
		AudioManager.onModeChange.dispatch(mode);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onHeadphonesChanged(connected:Bool):Void {
		AudioManager.onHeadphonesChange.dispatch(connected);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onDeviceChanged(added:Bool, content:String):Void {
		AudioManager.__device(added, content);
	}
}
#end
