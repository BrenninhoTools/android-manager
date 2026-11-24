package androidmanager;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputDevice;
import android.view.PointerIcon;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import org.haxe.extension.Extension;
import org.json.JSONArray;
import org.json.JSONObject;

public class InputTools {

    private static View findSurface(View view) {
        if (view instanceof SurfaceView)
            return view;

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findSurface(group.getChildAt(i));
                if (found != null)
                    return found;
            }
        }

        return null;
    }

    private static View touchView() {
        View view = findSurface(Extension.mainView);
        if (view == null && Utils.activity() != null)
            view = findSurface(Utils.activity().getWindow().getDecorView());

        return view != null ? view : Extension.mainView;
    }

    public static boolean requestUnbufferedDispatch(final boolean enable) {
        if (Build.VERSION.SDK_INT < 30)
            return false;

        Utils.runOnUi(() -> {
            View view = touchView();
            if (view != null)
                view.requestUnbufferedDispatch(enable ? InputDevice.SOURCE_TOUCHSCREEN : 0);
        });
        return true;
    }

    private static ViewConfiguration configuration() {
        Context ctx = Utils.context();
        return ctx != null ? ViewConfiguration.get(ctx) : null;
    }

    public static int getTouchSlop() {
        ViewConfiguration config = configuration();
        return config != null ? config.getScaledTouchSlop() : 16;
    }

    public static int getDoubleTapSlop() {
        ViewConfiguration config = configuration();
        return config != null ? config.getScaledDoubleTapSlop() : 100;
    }

    public static int getMinFlingVelocity() {
        ViewConfiguration config = configuration();
        return config != null ? config.getScaledMinimumFlingVelocity() : 50;
    }

    public static int getMaxFlingVelocity() {
        ViewConfiguration config = configuration();
        return config != null ? config.getScaledMaximumFlingVelocity() : 8000;
    }

    public static int getTapTimeout() {
        return ViewConfiguration.getTapTimeout();
    }

    public static int getDoubleTapTimeout() {
        return ViewConfiguration.getDoubleTapTimeout();
    }

    public static int getLongPressTimeout() {
        return ViewConfiguration.getLongPressTimeout();
    }

    public static float getDensity() {
        Context ctx = Utils.context();
        if (ctx == null)
            return 1f;

        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        return metrics != null ? metrics.density : 1f;
    }

    public static int getMaxTouchPoints() {
        Context ctx = Utils.context();
        if (ctx == null)
            return 1;

        PackageManager pm = ctx.getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_JAZZHAND))
            return 5;
        if (pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT))
            return 2;
        if (pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH))
            return 2;
        return pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) ? 1 : 0;
    }

    public static boolean hasSource(int source) {
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id);
            if (device == null || device.isVirtual())
                continue;

            if ((device.getSources() & source) == source)
                return true;
        }

        return false;
    }

    public static boolean isHapticFeedbackEnabled() {
        Context ctx = Utils.context();
        if (ctx == null)
            return true;

        try {
            return Settings.System.getInt(ctx.getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0;
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean isPhysicalKeyboardConnected() {
        Context ctx = Utils.context();
        if (ctx == null)
            return false;

        Configuration config = ctx.getResources().getConfiguration();
        if (config.keyboard == Configuration.KEYBOARD_QWERTY && config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)
            return true;

        for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id);
            if (device == null || device.isVirtual())
                continue;

            if (device.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC && isExternal(device))
                return true;
        }

        return false;
    }

    private static boolean isExternal(InputDevice device) {
        if (Build.VERSION.SDK_INT >= 29)
            return device.isExternal();

        return !device.isVirtual() && device.getId() > 0;
    }

    public static boolean setPointerVisible(final boolean visible) {
        if (Build.VERSION.SDK_INT < 24)
            return false;

        Utils.runOnUi(() -> {
            View view = touchView();
            Context ctx = Utils.context();
            if (view == null || ctx == null)
                return;

            view.setPointerIcon(visible ? null : PointerIcon.getSystemIcon(ctx, PointerIcon.TYPE_NULL));
        });
        return true;
    }

    private static JSONArray typesOf(int sources) {
        JSONArray types = new JSONArray();
        if ((sources & InputDevice.SOURCE_TOUCHSCREEN) == InputDevice.SOURCE_TOUCHSCREEN)
            types.put("touchscreen");
        if ((sources & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE)
            types.put("mouse");
        if ((sources & InputDevice.SOURCE_STYLUS) == InputDevice.SOURCE_STYLUS)
            types.put("stylus");
        if ((sources & InputDevice.SOURCE_TOUCHPAD) == InputDevice.SOURCE_TOUCHPAD)
            types.put("touchpad");
        if ((sources & InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD)
            types.put("keyboard");
        if ((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
            types.put("gamepad");
        if ((sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
            types.put("joystick");
        return types;
    }

    public static String getInputDevices() {
        JSONArray list = new JSONArray();

        for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id);
            if (device == null)
                continue;

            try {
                JSONObject json = new JSONObject();
                json.put("id", device.getId());
                json.put("name", device.getName());
                json.put("vendorId", device.getVendorId());
                json.put("productId", device.getProductId());
                json.put("sources", device.getSources());
                json.put("types", typesOf(device.getSources()));
                json.put("virtual", device.isVirtual());
                json.put("external", isExternal(device));
                json.put("alphabeticKeyboard", device.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC);
                json.put("vibrator", device.getVibrator() != null && device.getVibrator().hasVibrator());
                list.put(json);
            } catch (Exception e) {
                Log.e("AndroidManager", e.toString());
            }
        }

        return list.toString();
    }
}
