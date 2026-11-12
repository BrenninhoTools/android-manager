package androidmanager.content;

#if android
import androidmanager.jni.JNICache;
#end

class Clipboard {
	private static inline final CLASS:String = "java/androidmanager/Clipboard";

	public static var text(get, set):String;
	public static var hasText(get, never):Bool;

	public static function setText(value:String, label:String = ""):Void {
		#if android
		JNICache.call(CLASS, "setText", "(Ljava/lang/String;Ljava/lang/String;)V", [value == null ? "" : value, label]);
		#end
	}

	public static function clear():Void {
		#if android
		JNICache.call(CLASS, "clear", "()V", []);
		#end
	}

	private static function get_text():String {
		#if android
		var value:Null<String> = JNICache.call(CLASS, "getText", "()Ljava/lang/String;", [], "");
		return value != null ? value : "";
		#else
		return "";
		#end
	}

	private static function set_text(value:String):String {
		setText(value);
		return value;
	}

	private static function get_hasText():Bool {
		#if android
		return JNICache.call(CLASS, "hasText", "()Z", [], false);
		#else
		return false;
		#end
	}
}
