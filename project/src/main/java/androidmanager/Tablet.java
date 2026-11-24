package androidmanager;

import android.app.Activity;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import org.haxe.lime.HaxeObject;
import org.json.JSONObject;

public class Tablet {

    private static final int TYPE_HINGE_ANGLE = 36;

    private static HaxeObject listener = null;
    private static ComponentCallbacks callbacks = null;
    private static SensorEventListener hingeListener = null;
    private static String lastInfo = null;
    private static String lastPosture = null;
    private static volatile float hingeAngle = -1f;

    private static Context context() {
        return Utils.context();
    }

    private static Configuration configuration() {
        Context ctx = context();
        return ctx != null ? ctx.getResources().getConfiguration() : null;
    }

    private static boolean hasFeature(String feature) {
        Context ctx = context();
        if (ctx == null)
            return false;

        try {
            return ctx.getPackageManager().hasSystemFeature(feature);
        } catch (Exception e) {
            return false;
        }
    }

    private static Sensor hingeSensor() {
        Context ctx = context();
        if (ctx == null || Build.VERSION.SDK_INT < 30)
            return null;

        SensorManager manager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        return manager != null ? manager.getDefaultSensor(TYPE_HINGE_ANGLE) : null;
    }

    public static boolean isFoldable() {
        return hasFeature("android.hardware.sensor.hinge_angle") || hingeSensor() != null;
    }

    public static boolean isLargeScreen() {
        Configuration config = configuration();
        if (config == null)
            return false;

        return config.smallestScreenWidthDp >= 600
            || (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isDesktop() {
        return hasFeature("android.hardware.type.pc") || Device.isChromebook() || Device.isDeXMode();
    }

    public static boolean hasTouchscreen() {
        return hasFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }

    public static String getFormFactor() {
        if (Device.isWatch())
            return "watch";
        if (Device.isAutomotive())
            return "automotive";
        if (Device.isAndroidTV())
            return "tv";
        if (isDesktop())
            return "desktop";
        if (isFoldable())
            return "foldable";
        if (isLargeScreen())
            return "tablet";
        return "phone";
    }

    public static float getHingeAngle() {
        return hingeAngle;
    }

    public static String getPosture() {
        return postureOf(hingeAngle);
    }

    private static String postureOf(float angle) {
        if (angle < 0)
            return "unknown";
        if (angle < 20)
            return "closed";
        if (angle < 160)
            return "half_opened";
        return "flat";
    }

    private static String widthClass(int dp) {
        if (dp < 600)
            return "compact";
        if (dp < 840)
            return "medium";
        if (dp < 1200)
            return "expanded";
        if (dp < 1600)
            return "large";
        return "extra_large";
    }

    private static String heightClass(int dp) {
        if (dp < 480)
            return "compact";
        if (dp < 900)
            return "medium";
        return "expanded";
    }

    @SuppressWarnings("deprecation")
    private static Display display() {
        Activity act = Utils.activity();
        if (act == null)
            return null;

        if (Build.VERSION.SDK_INT >= 30)
            return act.getDisplay();

        return act.getWindowManager().getDefaultDisplay();
    }

    @SuppressWarnings("deprecation")
    private static JSONObject buildInfo() throws Exception {
        JSONObject json = new JSONObject();
        Context ctx = context();
        Configuration config = configuration();
        Activity act = Utils.activity();

        int widthDp = config != null ? config.screenWidthDp : 0;
        int heightDp = config != null ? config.screenHeightDp : 0;
        int smallestDp = config != null ? config.smallestScreenWidthDp : 0;

        DisplayMetrics metrics = new DisplayMetrics();
        Display display = display();
        if (display != null)
            display.getRealMetrics(metrics);
        else if (ctx != null)
            metrics = ctx.getResources().getDisplayMetrics();

        int windowWidth = 0;
        int windowHeight = 0;
        if (act != null) {
            if (Build.VERSION.SDK_INT >= 30) {
                Rect bounds = act.getWindowManager().getCurrentWindowMetrics().getBounds();
                windowWidth = bounds.width();
                windowHeight = bounds.height();
            } else if (display != null) {
                Point size = new Point();
                display.getSize(size);
                windowWidth = size.x;
                windowHeight = size.y;
            }
        }

        float xdpi = metrics.xdpi > 0 ? metrics.xdpi : metrics.densityDpi;
        float ydpi = metrics.ydpi > 0 ? metrics.ydpi : metrics.densityDpi;
        double inchesX = xdpi > 0 ? metrics.widthPixels / (double) xdpi : 0;
        double inchesY = ydpi > 0 ? metrics.heightPixels / (double) ydpi : 0;
        double diagonal = Math.sqrt(inchesX * inchesX + inchesY * inchesY);

        json.put("formFactor", getFormFactor());
        json.put("largeScreen", isLargeScreen());
        json.put("foldable", isFoldable());
        json.put("desktop", isDesktop());
        json.put("touchscreen", hasTouchscreen());
        json.put("widthDp", widthDp);
        json.put("heightDp", heightDp);
        json.put("smallestWidthDp", smallestDp);
        json.put("widthClass", widthClass(widthDp));
        json.put("heightClass", heightClass(heightDp));
        json.put("screenWidth", metrics.widthPixels);
        json.put("screenHeight", metrics.heightPixels);
        json.put("windowWidth", windowWidth);
        json.put("windowHeight", windowHeight);
        json.put("density", (double) metrics.density);
        json.put("densityDpi", metrics.densityDpi);
        json.put("xdpi", (double) xdpi);
        json.put("ydpi", (double) ydpi);
        json.put("diagonalInches", Math.round(diagonal * 100.0) / 100.0);
        json.put("orientation", config != null ? config.orientation : 0);
        json.put("multiWindow", Device.isInMultiWindowMode());
        json.put("physicalKeyboard", InputTools.isPhysicalKeyboardConnected());
        json.put("hingeAngle", (double) hingeAngle);
        json.put("posture", postureOf(hingeAngle));
        return json;
    }

    public static String getScreenInfo() {
        String result = Utils.callOnUi(() -> buildInfo().toString(), "{}");
        return result != null ? result : "{}";
    }

    public static float millimetersToPixels(float millimeters) {
        Context ctx = context();
        if (ctx == null)
            return millimeters * 160f / 25.4f;

        DisplayMetrics metrics = new DisplayMetrics();
        Display display = display();
        if (display != null)
            display.getRealMetrics(metrics);
        else
            metrics = ctx.getResources().getDisplayMetrics();

        float dpi = metrics.xdpi > 0 ? (metrics.xdpi + Math.max(1f, metrics.ydpi)) / 2f : metrics.densityDpi;
        if (dpi < 60 || dpi > 1200)
            dpi = metrics.densityDpi;

        return millimeters * dpi / 25.4f;
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

    private static void notifyChanged() {
        Utils.runOnUi(() -> {
            try {
                String info = buildInfo().toString();
                if (info.equals(lastInfo))
                    return;

                lastInfo = info;
                dispatch("onScreenChanged", info);
            } catch (Exception e) {
                Log.e("AndroidManager", e.toString());
            }
        });
    }

    public static void startListening(final HaxeObject callback) {
        listener = callback;

        Utils.runOnUi(() -> {
            Context ctx = context();
            if (ctx == null)
                return;

            if (callbacks == null) {
                callbacks = new ComponentCallbacks() {
                    @Override
                    public void onConfigurationChanged(Configuration config) {
                        notifyChanged();
                    }

                    @Override
                    public void onLowMemory() {}
                };
                ctx.getApplicationContext().registerComponentCallbacks(callbacks);
            }

            Sensor sensor = hingeSensor();
            if (sensor != null && hingeListener == null) {
                SensorManager manager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
                hingeListener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        if (event.values == null || event.values.length == 0)
                            return;

                        hingeAngle = event.values[0];
                        String posture = postureOf(hingeAngle);
                        if (!posture.equals(lastPosture)) {
                            lastPosture = posture;
                            dispatch("onPostureChanged", posture, (double) hingeAngle);
                            notifyChanged();
                        }
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
                };
                manager.registerListener(hingeListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }

            notifyChanged();
        });
    }

    public static void stopListening() {
        Utils.runOnUi(() -> {
            Context ctx = context();
            if (ctx != null && callbacks != null)
                ctx.getApplicationContext().unregisterComponentCallbacks(callbacks);
            callbacks = null;

            if (ctx != null && hingeListener != null) {
                SensorManager manager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
                if (manager != null)
                    manager.unregisterListener(hingeListener);
            }
            hingeListener = null;
            lastInfo = null;
            lastPosture = null;
            listener = null;
        });
    }
}
