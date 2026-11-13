package androidmanager.battery;

#if android
import androidmanager.jni.JNICache;
#end
import haxe.Json;
import lime.app.Event;

enum abstract BatteryStatus(Int) from Int to Int {
	var Unknown = 1;
	var Charging = 2;
	var Discharging = 3;
	var NotCharging = 4;
	var Full = 5;
}

enum abstract BatteryHealth(Int) from Int to Int {
	var Unknown = 1;
	var Good = 2;
	var Overheat = 3;
	var Dead = 4;
	var OverVoltage = 5;
	var UnspecifiedFailure = 6;
	var Cold = 7;
}

enum abstract BatteryPlugged(Int) from Int to Int {
	var Unplugged = 0;
	var AC = 1;
	var USB = 2;
	var Wireless = 4;
	var Dock = 8;
}

enum abstract ChargeType(String) from String to String {
	var None = "none";
	var AC = "ac";
	var USB = "usb";
	var Wireless = "wireless";
	var Dock = "dock";
}

enum abstract ThermalStatus(Int) from Int to Int {
	var Unsupported = -1;
	var None = 0;
	var Light = 1;
	var Moderate = 2;
	var Severe = 3;
	var Critical = 4;
	var Emergency = 5;
	var Shutdown = 6;
}

typedef BatteryInfo = {
	var level:Int;
	var status:BatteryStatus;
	var health:BatteryHealth;
	var plugged:BatteryPlugged;
	var temperature:Float;
	var charging:Bool;
	var low:Bool;
	var powerSave:Bool;
}

typedef BatteryDetails = {
	> BatteryInfo,
	var levelPrecise:Float;
	var chargeType:ChargeType;
	var voltage:Int;
	var technology:String;
	var present:Bool;
	var full:Bool;
	var deviceIdle:Bool;
	var ignoringOptimizations:Bool;
	var currentNow:Float;
	var currentAverage:Float;
	var chargeCounter:Float;
	var energyCounter:Float;
	var powerWatts:Float;
	var designCapacity:Float;
	var estimatedCapacity:Float;
	var cycleCount:Int;
	var chargeTimeRemaining:Float;
	var dischargeTimeRemaining:Float;
	var thermalStatus:ThermalStatus;
}

private typedef LevelAlert = {
	var level:Int;
	var callback:Int->Void;
	var triggered:Bool;
	var repeat:Bool;
}

class Battery {
	private static inline final CLASS:String = "androidmanager/Battery";

	public static var onBatteryChanged:(info:BatteryInfo) -> Void = null;

	public static var onChange(default, null):Event<BatteryDetails->Void> = new Event<BatteryDetails->Void>();
	public static var onLevelChange(default, null):Event<Int->Void> = new Event<Int->Void>();
	public static var onChargingChange(default, null):Event<Bool->Void> = new Event<Bool->Void>();
	public static var onPowerConnected(default, null):Event<Bool->Void> = new Event<Bool->Void>();
	public static var onLow(default, null):Event<Bool->Void> = new Event<Bool->Void>();
	public static var onPowerSaveChange(default, null):Event<Bool->Void> = new Event<Bool->Void>();
	public static var onDeviceIdleChange(default, null):Event<Bool->Void> = new Event<Bool->Void>();
	public static var onThermalChange(default, null):Event<ThermalStatus->Void> = new Event<ThermalStatus->Void>();
	public static var onOverheat(default, null):Event<Float->Void> = new Event<Float->Void>();

	public static var overheatTemperature:Float = 42;
	public static var last(default, null):Null<BatteryDetails> = null;

	public static var level(get, never):Int;
	public static var levelPrecise(get, never):Float;
	public static var charging(get, never):Bool;
	public static var powerSave(get, never):Bool;
	public static var temperature(get, never):Float;
	public static var thermalStatus(get, never):ThermalStatus;
	public static var details(get, never):Null<BatteryDetails>;

	private static var alerts:Array<LevelAlert> = [];
	private static var initialized:Bool = false;
	private static var overheated:Bool = false;

	public static function init():Void {
		#if android
		if (initialized)
			return;

		initialized = true;
		call("setCallback", "(Lorg/haxe/lime/HaxeObject;)V", [new BatteryHandler()]);
		#end
	}

	public static function getLevel():Int
		return call("getLevel", "()I", [], -1);

	public static function getStatus():BatteryStatus
		return call("getStatus", "()I", [], 1);

	public static function getHealth():BatteryHealth
		return call("getHealth", "()I", [], 1);

	public static function getPlugged():BatteryPlugged
		return call("getPlugged", "()I", [], 0);

	public static function getChargeType():ChargeType
		return call("getChargeType", "()Ljava/lang/String;", [], "none");

	public static function getTemperature():Float
		return call("getTemperature", "()F", [], -1.0);

	public static function getVoltage():Int
		return call("getVoltage", "()I", [], -1);

	public static function getTechnology():Null<String>
		return call("getTechnology", "()Ljava/lang/String;", [], null);

	public static function isCharging():Bool
		return call("isCharging", "()Z", [], false);

	public static function isLow():Bool
		return call("isLow", "()Z", [], false);

	public static function isPowerSaveMode():Bool
		return call("isPowerSaveMode", "()Z", [], false);

	public static function isDeviceIdle():Bool
		return call("isDeviceIdle", "()Z", [], false);

	public static function isIgnoringBatteryOptimizations():Bool
		return call("isIgnoringBatteryOptimizations", "()Z", [], true);

	public static function getCurrentNow():Float
		return call("getCurrentNow", "()D", [], 0.0);

	public static function getCurrentAverage():Float
		return call("getCurrentAverage", "()D", [], 0.0);

	public static function getChargeCounter():Float
		return call("getChargeCounter", "()D", [], -1.0);

	public static function getEnergyCounter():Float
		return call("getEnergyCounter", "()D", [], -1.0);

	public static function getDesignCapacity():Float
		return call("getDesignCapacity", "()D", [], -1.0);

	public static function getEstimatedCapacity():Float
		return call("getEstimatedCapacity", "()D", [], -1.0);

	public static function getHealthPercent():Float {
		var design = getDesignCapacity();
		var estimated = getEstimatedCapacity();
		if (design <= 0 || estimated <= 0)
			return -1;

		return Math.min(100, estimated / design * 100);
	}

	public static function getCycleCount():Int
		return call("getCycleCount", "()I", [], -1);

	public static function getChargeTimeRemaining():Float
		return call("getChargeTimeRemainingMs", "()D", [], -1.0);

	public static function getDischargeTimeRemaining():Float
		return call("getDischargeTimeRemaining", "()D", [], -1.0);

	public static function getThermalHeadroom(forecastSeconds:Int = 0):Float
		return call("getThermalHeadroom", "(I)F", [forecastSeconds], -1.0);

	public static function getSnapshot():BatteryInfo {
		var info = getDetails();
		if (info != null)
			return info;

		return {
			level: getLevel(),
			status: getStatus(),
			health: getHealth(),
			plugged: getPlugged(),
			temperature: getTemperature(),
			charging: isCharging(),
			low: isLow(),
			powerSave: isPowerSaveMode()
		};
	}

	public static function getDetails():Null<BatteryDetails> {
		var content:String = call("getInfo", "()Ljava/lang/String;", [], null);
		if (content == null)
			return null;

		try {
			return Json.parse(content);
		} catch (e:Dynamic) {
			return null;
		}
	}

	public static function startListening():Void {
		#if android
		init();
		call("startListening", "()V", []);
		#end
	}

	public static function stopListening():Void
		call("stopListening", "()V", []);

	public static function isListening():Bool
		return call("isListening", "()Z", [], false);

	public static function addLevelAlert(level:Int, callback:Int->Void, repeat:Bool = true):Void {
		if (callback == null)
			return;

		alerts.push({level: level, callback: callback, triggered: false, repeat: repeat});
		startListening();
	}

	public static function removeLevelAlert(callback:Int->Void):Void {
		for (alert in alerts.copy())
			if (Reflect.compareMethods(alert.callback, callback))
				alerts.remove(alert);
	}

	public static function clearLevelAlerts():Void
		alerts = [];

	public static function openBatteryOptimizationSettings():Bool
		return call("openBatteryOptimizationSettings", "()Z", [], false);

	public static function openBatteryUsage():Bool
		return call("openBatteryUsage", "()Z", [], false);

	public static function openBatterySaverSettings():Bool
		return call("openBatterySaverSettings", "()Z", [], false);

	@:noCompletion
	public static function __info(content:String):Void {
		var info:BatteryDetails = null;
		try {
			info = Json.parse(content);
		} catch (e:Dynamic) {}

		if (info == null)
			return;

		var previous = last;
		last = info;

		var levelChanged = previous == null || previous.level != info.level;
		var chargingChanged = previous == null || previous.charging != info.charging;

		if (levelChanged)
			onLevelChange.dispatch(info.level);

		if (chargingChanged)
			onChargingChange.dispatch(info.charging);

		if (levelChanged || chargingChanged)
			checkAlerts(info.level, info.charging);

		if (info.temperature >= overheatTemperature && !overheated) {
			overheated = true;
			onOverheat.dispatch(info.temperature);
		} else if (info.temperature < overheatTemperature - 2) {
			overheated = false;
		}

		onChange.dispatch(info);

		if (onBatteryChanged != null)
			onBatteryChanged(info);
	}

	private static function checkAlerts(current:Int, isCharging:Bool):Void {
		for (alert in alerts.copy()) {
			if (current > alert.level || isCharging) {
				alert.triggered = false;
				continue;
			}

			if (!alert.triggered) {
				alert.triggered = true;
				alert.callback(current);
				if (!alert.repeat)
					alerts.remove(alert);
			}
		}
	}

	private static function get_level():Int
		return last != null ? last.level : getLevel();

	private static function get_levelPrecise():Float
		return call("getLevelPrecise", "()D", [], -1.0);

	private static function get_charging():Bool
		return last != null ? last.charging : isCharging();

	private static function get_powerSave():Bool
		return isPowerSaveMode();

	private static function get_temperature():Float
		return getTemperature();

	private static function get_thermalStatus():ThermalStatus
		return call("getThermalStatus", "()I", [], -1);

	private static function get_details():Null<BatteryDetails>
		return getDetails();

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
private class BatteryHandler #if (lime >= "8.0.0") implements lime.system.JNI.JNISafety #end {
	public function new() {}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onBatteryInfo(content:String):Void {
		Battery.__info(content);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onPowerConnected(connected:Bool):Void {
		Battery.onPowerConnected.dispatch(connected);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onBatteryLow(low:Bool):Void {
		Battery.onLow.dispatch(low);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onPowerSaveChanged(enabled:Bool):Void {
		Battery.onPowerSaveChange.dispatch(enabled);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onDeviceIdleChanged(idle:Bool):Void {
		Battery.onDeviceIdleChange.dispatch(idle);
	}

	@:keep
	#if (lime >= "8.0.0") @:runOnMainThread #end
	public function onThermalChanged(status:Int):Void {
		Battery.onThermalChange.dispatch(status);
	}
}
#end
