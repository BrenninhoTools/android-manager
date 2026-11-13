package androidmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import org.haxe.extension.Extension;
import org.haxe.lime.HaxeObject;
import org.json.JSONObject;

public class Battery {

    private static HaxeObject listener = null;
    private static BroadcastReceiver receiver = null;
    private static Object thermalListener = null;
    private static boolean listening = false;
    private static String lastInfo = null;
    private static double designCapacity = -2;

    private static Context context() {
        return Extension.mainContext != null ? Extension.mainContext : Extension.mainActivity;
    }

    private static BatteryManager manager() {
        Context ctx = context();
        return ctx != null ? (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE) : null;
    }

    private static PowerManager power() {
        Context ctx = context();
        return ctx != null ? (PowerManager) ctx.getSystemService(Context.POWER_SERVICE) : null;
    }

    private static Intent batteryIntent() {
        Context ctx = context();
        if (ctx == null)
            return null;

        try {
            return ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        } catch (Exception e) {
            return null;
        }
    }

    public static void init() {
    }

    public static void setCallback(HaxeObject callback) {
        listener = callback;
    }

    public static int getLevel() {
        double level = getLevelPrecise();
        return level < 0 ? -1 : (int) Math.round(level);
    }

    public static double getLevelPrecise() {
        Intent intent = batteryIntent();
        if (intent == null)
            return -1;

        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0)
            return -1;

        return level * 100.0 / scale;
    }

    public static int getStatus() {
        Intent intent = batteryIntent();
        return intent != null ? intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN) : BatteryManager.BATTERY_STATUS_UNKNOWN;
    }

    public static int getHealth() {
        Intent intent = batteryIntent();
        return intent != null ? intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) : BatteryManager.BATTERY_HEALTH_UNKNOWN;
    }

    public static int getPlugged() {
        Intent intent = batteryIntent();
        return intent != null ? intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) : 0;
    }

    public static float getTemperature() {
        Intent intent = batteryIntent();
        if (intent == null)
            return -1f;

        int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
        return temp == Integer.MIN_VALUE ? -1f : temp / 10.0f;
    }

    public static int getVoltage() {
        Intent intent = batteryIntent();
        return intent != null ? intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) : -1;
    }

    public static String getTechnology() {
        Intent intent = batteryIntent();
        return intent != null ? intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) : null;
    }

    public static boolean isPresent() {
        Intent intent = batteryIntent();
        return intent != null && intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true);
    }

    public static boolean isCharging() {
        int status = getStatus();
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
    }

    public static boolean isLow() {
        Intent intent = batteryIntent();
        if (intent != null && Build.VERSION.SDK_INT >= 28 && intent.hasExtra(BatteryManager.EXTRA_BATTERY_LOW))
            return intent.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false);

        int level = getLevel();
        return level >= 0 && level <= 15 && !isCharging();
    }

    public static boolean isPowerSaveMode() {
        PowerManager pm = power();
        return pm != null && pm.isPowerSaveMode();
    }

    public static boolean isIgnoringBatteryOptimizations() {
        PowerManager pm = power();
        if (pm == null || Build.VERSION.SDK_INT < 23)
            return true;

        return pm.isIgnoringBatteryOptimizations(context().getPackageName());
    }

    public static boolean isDeviceIdle() {
        PowerManager pm = power();
        return pm != null && Build.VERSION.SDK_INT >= 23 && pm.isDeviceIdleMode();
    }

    public static int getThermalStatus() {
        PowerManager pm = power();
        return pm != null && Build.VERSION.SDK_INT >= 29 ? pm.getCurrentThermalStatus() : -1;
    }

    public static float getThermalHeadroom(int forecastSeconds) {
        PowerManager pm = power();
        if (pm == null || Build.VERSION.SDK_INT < 30)
            return -1f;

        float value = pm.getThermalHeadroom(Math.max(0, Math.min(60, forecastSeconds)));
        return Float.isNaN(value) ? -1f : value;
    }

    public static long getChargeTimeRemaining() {
        BatteryManager bm = manager();
        return bm != null && Build.VERSION.SDK_INT >= 28 ? bm.computeChargeTimeRemaining() : -1;
    }

    public static double getChargeTimeRemainingMs() {
        return (double) getChargeTimeRemaining();
    }

    public static double getDischargeTimeRemaining() {
        PowerManager pm = power();
        if (pm == null || Build.VERSION.SDK_INT < 31)
            return -1;

        try {
            java.time.Duration duration = pm.getBatteryDischargePrediction();
            return duration != null ? (double) duration.toMillis() : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private static long property(int id) {
        BatteryManager bm = manager();
        if (bm == null)
            return Long.MIN_VALUE;

        long value = bm.getLongProperty(id);
        return value == Long.MIN_VALUE || value == Integer.MIN_VALUE ? Long.MIN_VALUE : value;
    }

    public static double getCurrentNow() {
        long value = property(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        return value == Long.MIN_VALUE ? 0 : (double) value;
    }

    public static double getCurrentAverage() {
        long value = property(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
        return value == Long.MIN_VALUE ? 0 : (double) value;
    }

    public static double getChargeCounter() {
        long value = property(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        return value == Long.MIN_VALUE ? -1 : (double) value;
    }

    public static double getEnergyCounter() {
        long value = property(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
        return value == Long.MIN_VALUE ? -1 : (double) value;
    }

    public static double getDesignCapacity() {
        if (designCapacity != -2)
            return designCapacity;

        designCapacity = -1;
        try {
            Class<?> profileClass = Class.forName("com.android.internal.os.PowerProfile");
            Object profile = profileClass.getConstructor(Context.class).newInstance(context());
            Object value = profileClass.getMethod("getBatteryCapacity").invoke(profile);
            if (value instanceof Double && (Double) value > 0)
                designCapacity = (Double) value;
        } catch (Throwable ignored) {
        }
        return designCapacity;
    }

    public static double getEstimatedCapacity() {
        double counter = getChargeCounter();
        double level = getLevelPrecise();
        if (counter <= 0 || level <= 0)
            return -1;

        return (counter / 1000.0) / (level / 100.0);
    }

    public static int getCycleCount() {
        Intent intent = batteryIntent();
        if (intent == null || Build.VERSION.SDK_INT < 34)
            return -1;

        return intent.getIntExtra("android.os.extra.CYCLE_COUNT", -1);
    }

    public static String getChargeType() {
        switch (getPlugged()) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                return "ac";
            case BatteryManager.BATTERY_PLUGGED_USB:
                return "usb";
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                return "wireless";
            case 8:
                return "dock";
            default:
                return "none";
        }
    }

    public static String getInfo() {
        JSONObject json = new JSONObject();
        try {
            Intent intent = batteryIntent();
            int status = intent != null ? intent.getIntExtra(BatteryManager.EXTRA_STATUS, 1) : 1;
            int voltage = getVoltage();
            double current = getCurrentNow();

            json.put("level", getLevel());
            json.put("levelPrecise", getLevelPrecise());
            json.put("status", status);
            json.put("health", getHealth());
            json.put("plugged", getPlugged());
            json.put("chargeType", getChargeType());
            json.put("temperature", (double) getTemperature());
            json.put("voltage", voltage);
            json.put("technology", getTechnology() != null ? getTechnology() : "");
            json.put("present", isPresent());
            json.put("charging", status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
            json.put("full", status == BatteryManager.BATTERY_STATUS_FULL);
            json.put("low", isLow());
            json.put("powerSave", isPowerSaveMode());
            json.put("deviceIdle", isDeviceIdle());
            json.put("ignoringOptimizations", isIgnoringBatteryOptimizations());
            json.put("currentNow", current);
            json.put("currentAverage", getCurrentAverage());
            json.put("chargeCounter", getChargeCounter());
            json.put("energyCounter", getEnergyCounter());
            json.put("powerWatts", voltage > 0 ? Math.abs((voltage / 1000.0) * (current / 1000000.0)) : 0);
            json.put("designCapacity", getDesignCapacity());
            json.put("estimatedCapacity", getEstimatedCapacity());
            json.put("cycleCount", getCycleCount());
            json.put("chargeTimeRemaining", (double) getChargeTimeRemaining());
            json.put("dischargeTimeRemaining", getDischargeTimeRemaining());
            json.put("thermalStatus", getThermalStatus());
        } catch (Exception e) {
            Log.e("AndroidManager", e.toString());
        }
        return json.toString();
    }

    private static void dispatch(String method, Object... args) {
        HaxeObject target = listener;
        if (target == null)
            return;

        try {
            target.call(method, args);
        } catch (Exception e) {
            Log.e("AndroidManager", e.toString());
        }
    }

    private static String stateKey(Intent intent) {
        if (intent == null)
            return "";

        return intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) + "|"
            + intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) + "|"
            + intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) + "|"
            + intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) + "|"
            + (intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10);
    }

    public static void startListening() {
        startListeningWith(listener);
    }

    public static void startListeningWith(HaxeObject callback) {
        if (callback != null)
            listener = callback;

        Context ctx = context();
        if (listening || ctx == null)
            return;

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null)
                    return;

                switch (action) {
                    case Intent.ACTION_BATTERY_CHANGED:
                        String key = stateKey(intent);
                        if (key.equals(lastInfo))
                            return;
                        lastInfo = key;
                        dispatch("onBatteryInfo", getInfo());
                        break;
                    case Intent.ACTION_POWER_CONNECTED:
                        dispatch("onPowerConnected", true);
                        break;
                    case Intent.ACTION_POWER_DISCONNECTED:
                        dispatch("onPowerConnected", false);
                        break;
                    case Intent.ACTION_BATTERY_LOW:
                        dispatch("onBatteryLow", true);
                        break;
                    case Intent.ACTION_BATTERY_OKAY:
                        dispatch("onBatteryLow", false);
                        break;
                    case PowerManager.ACTION_POWER_SAVE_MODE_CHANGED:
                        dispatch("onPowerSaveChanged", isPowerSaveMode());
                        break;
                    default:
                        if (Build.VERSION.SDK_INT >= 23 && PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED.equals(action))
                            dispatch("onDeviceIdleChanged", isDeviceIdle());
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        if (Build.VERSION.SDK_INT >= 23)
            filter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);

        try {
            if (Build.VERSION.SDK_INT >= 33)
                ctx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
            else
                ctx.registerReceiver(receiver, filter);
        } catch (Exception e) {
            Log.e("AndroidManager", e.toString());
            receiver = null;
            return;
        }

        PowerManager pm = power();
        if (pm != null && Build.VERSION.SDK_INT >= 29) {
            PowerManager.OnThermalStatusChangedListener thermal = new PowerManager.OnThermalStatusChangedListener() {
                @Override
                public void onThermalStatusChanged(int status) {
                    dispatch("onThermalChanged", status);
                }
            };
            pm.addThermalStatusListener(thermal);
            thermalListener = thermal;
        }

        listening = true;
    }

    public static void stopListening() {
        Context ctx = context();
        if (!listening || ctx == null)
            return;

        try {
            if (receiver != null)
                ctx.unregisterReceiver(receiver);
        } catch (Exception ignored) {
        }

        PowerManager pm = power();
        if (pm != null && thermalListener != null && Build.VERSION.SDK_INT >= 29)
            pm.removeThermalStatusListener((PowerManager.OnThermalStatusChangedListener) thermalListener);

        receiver = null;
        thermalListener = null;
        lastInfo = null;
        listening = false;
    }

    public static boolean isListening() {
        return listening;
    }

    public static boolean openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT < 23 || Extension.mainActivity == null)
            return false;

        try {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Extension.mainActivity.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean openBatteryUsage() {
        if (Extension.mainActivity == null)
            return false;

        try {
            Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Extension.mainActivity.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean openBatterySaverSettings() {
        if (Extension.mainActivity == null)
            return false;

        try {
            Intent intent = new Intent(Build.VERSION.SDK_INT >= 22 ? Settings.ACTION_BATTERY_SAVER_SETTINGS : Intent.ACTION_POWER_USAGE_SUMMARY);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Extension.mainActivity.startActivity(intent);
            return true;
        } catch (Exception e) {
            return openBatteryUsage();
        }
    }
}
