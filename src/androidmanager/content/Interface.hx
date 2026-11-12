package androidmanager.content;

#if android
import androidmanager.jni.JNICache;
#end
import haxe.Json;

using StringTools;

enum abstract ToastDuration(Int) from Int to Int {
	var SHORT = 0;
	var LONG = 1;
}

enum abstract Gravity(Int) from Int to Int {
	var DEFAULT = -1;
	var CENTER = 17;
	var TOP = 48;
	var BOTTOM = 80;
	var LEFT = 3;
	var RIGHT = 5;
	var CENTER_HORIZONTAL = 1;
	var CENTER_VERTICAL = 16;
}

enum abstract DialogButton(Int) from Int to Int {
	var CANCELED = 0;
	var POSITIVE = 1;
	var NEGATIVE = 2;
	var NEUTRAL = 3;
}

enum abstract InputType(Int) from Int to Int {
	var TEXT = 0;
	var NUMBER = 1;
	var MULTILINE = 2;
	var PASSWORD = 3;
	var EMAIL = 4;
	var URL = 5;
}

typedef DialogOptions = {
	@:optional var positive:String;
	@:optional var negative:String;
	@:optional var neutral:String;
	@:optional var cancelable:Bool;
}

typedef StyledToastOptions = {
	@:optional var long:Bool;
	@:optional var backgroundColor:Int;
	@:optional var textColor:Int;
	@:optional var showIcon:Bool;
	@:optional var bottomOffset:Int;
}

class Interface {
	private static inline final CLASS:String = "androidmanager/Interface";

	public static function showToast(msg:String):Void
		invoke("showToast", "(Ljava/lang/String;)V", [msg]);

	public static function showStyledToast(msg:String, ?options:StyledToastOptions):Void {
		if (options == null)
			options = {};

		invoke("showStyledToast", "(Ljava/lang/String;ZIIZI)V", [
			msg,
			options.long == true,
			options.backgroundColor == null ? 0xFFFAFAFA : options.backgroundColor,
			options.textColor == null ? 0xFF111111 : options.textColor,
			options.showIcon != false,
			options.bottomOffset == null ? 64 : options.bottomOffset
		]);
	}

	public static function makeToast(msg:String, duration:ToastDuration = SHORT, gravity:Gravity = DEFAULT, xOffset:Int = 0, yOffset:Int = 0):Void
		invoke("makeToast", "(Ljava/lang/String;IIII)V", [msg, duration, gravity, xOffset, yOffset]);

	public static function showAlert(title:String, msg:String, btn:String = "OK", ?onClose:Void->Void):Void {
		#if android
		invoke("alert", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lorg/haxe/lime/HaxeObject;)V", [title, msg, btn, new DialogHandler(onClose)]);
		#end
	}

	public static function showConfirm(title:String, msg:String, yesLabel:String, noLabel:String, onYes:Void->Void, ?onNo:Void->Void):Void {
		#if android
		invoke("confirm", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lorg/haxe/lime/HaxeObject;Lorg/haxe/lime/HaxeObject;)V", [
			title,
			msg,
			yesLabel,
			noLabel,
			new DialogHandler(onYes),
			new DialogHandler(onNo)
		]);
		#end
	}

	public static function showDialog(title:String, msg:String, ?options:DialogOptions, ?onResult:DialogButton->Void):Void {
		#if android
		if (options == null)
			options = {};

		var handler = new DialogHandler(null);
		handler.result = onResult;

		invoke("dialog", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZLorg/haxe/lime/HaxeObject;)V", [
			title,
			msg,
			options.positive == null ? "OK" : options.positive,
			options.negative == null ? "" : options.negative,
			options.neutral == null ? "" : options.neutral,
			options.cancelable != false,
			handler
		]);
		#end
	}

	public static function showInput(title:String, ?msg:String, ?onInput:Null<String>->Void, ?hint:String, ?defaultText:String, type:InputType = TEXT,
			positive:String = "OK", negative:String = "Cancel"):Void {
		#if android
		var handler = new DialogHandler(null);
		handler.input = onInput;

		invoke("input", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Lorg/haxe/lime/HaxeObject;)V", [
			title,
			msg == null ? "" : msg,
			hint == null ? "" : hint,
			defaultText == null ? "" : defaultText,
			type,
			positive,
			negative,
			handler
		]);
		#end
	}

	public static function showList(title:String, items:Array<String>, ?onSelect:Int->Void):Void {
		#if android
		var handler = new DialogHandler(null);
		handler.select = onSelect;
		invoke("list", "(Ljava/lang/String;[Ljava/lang/String;Lorg/haxe/lime/HaxeObject;)V", [title, items, handler]);
		#end
	}

	public static function showSingleChoice(title:String, items:Array<String>, checked:Int = -1, ?onSelect:Int->Void, positive:String = "OK",
			negative:String = "Cancel"):Void {
		#if android
		var handler = new DialogHandler(null);
		handler.select = onSelect;
		invoke("singleChoice", "(Ljava/lang/String;[Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Lorg/haxe/lime/HaxeObject;)V", [
			title,
			items,
			checked,
			positive,
			negative,
			handler
		]);
		#end
	}

	public static function showMultiChoice(title:String, items:Array<String>, ?checked:Array<Int>, ?onSelect:Null<Array<Int>>->Void, positive:String = "OK",
			negative:String = "Cancel"):Void {
		#if android
		var handler = new DialogHandler(null);
		handler.multiSelect = onSelect;
		invoke("multiChoice", "(Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lorg/haxe/lime/HaxeObject;)V", [
			title,
			items,
			Json.stringify(checked == null ? [] : checked),
			positive,
			negative,
			handler
		]);
		#end
	}

	public static function showProgress(title:String, msg:String, indeterminate:Bool = true, max:Int = 100):Void
		invoke("showProgress", "(Ljava/lang/String;Ljava/lang/String;ZI)V", [title, msg, indeterminate, max]);

	public static function updateProgress(value:Int, ?msg:String):Void
		invoke("updateProgress", "(ILjava/lang/String;)V", [value, msg == null ? "" : msg]);

	public static function hideProgress():Void
		invoke("hideProgress", "()V", []);

	public static function requestSetting(action:String, code:Int = 1):Void {
		var fullAction = action.startsWith("android.settings.") ? action : "android.settings." + action;
		invoke("navigate", "(Ljava/lang/String;I)V", [fullAction, code]);
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
private class DialogHandler #if (lime >= "8.0.0") implements lime.system.JNI.JNISafety #end {
	public var trigger:Void->Void;
	public var result:DialogButton->Void;
	public var input:Null<String>->Void;
	public var select:Int->Void;
	public var multiSelect:Null<Array<Int>>->Void;

	public function new(trigger:Void->Void) {
		this.trigger = trigger;
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onTrigger():Void {
		if (trigger != null)
			trigger();
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onResult(button:Int):Void {
		if (result != null)
			result(button);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onInput(confirmed:Bool, text:String):Void {
		if (input != null)
			input(confirmed ? text : null);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onSelect(index:Int):Void {
		if (select != null)
			select(index);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onMultiSelect(confirmed:Bool, content:String):Void {
		if (multiSelect == null)
			return;

		if (!confirmed) {
			multiSelect(null);
			return;
		}

		try {
			multiSelect(Json.parse(content));
		} catch (e:Dynamic) {
			multiSelect([]);
		}
	}
}
#end
