package androidmanager;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager.LayoutParams;

public class WindowManager {

    private static boolean immersive = false;

    private static Window window() {
        Activity act = Utils.activity();
        return act != null ? act.getWindow() : null;
    }

    private static void setFlag(final int flag, final boolean enabled) {
        Utils.runOnUi(() -> {
            Window w = window();
            if (w == null)
                return;

            if (enabled)
                w.addFlags(flag);
            else
                w.clearFlags(flag);
        });
    }

    private static boolean hasFlag(int flag) {
        Window w = window();
        return w != null && (w.getAttributes().flags & flag) != 0;
    }

    public static void setSecure(boolean enabled) {
        setFlag(LayoutParams.FLAG_SECURE, enabled);
    }

    public static boolean isSecure() {
        return hasFlag(LayoutParams.FLAG_SECURE);
    }

    public static void setKeepScreenOn(boolean enabled) {
        setFlag(LayoutParams.FLAG_KEEP_SCREEN_ON, enabled);
    }

    public static boolean isKeepScreenOn() {
        return hasFlag(LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @SuppressWarnings("deprecation")
    public static void setImmersive(final boolean enabled) {
        immersive = enabled;
        Utils.runOnUi(() -> {
            Window w = window();
            if (w == null)
                return;

            if (Build.VERSION.SDK_INT >= 30) {
                WindowInsetsController controller = w.getInsetsController();
                if (controller == null)
                    return;

                if (enabled) {
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                } else {
                    controller.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                }
            } else {
                View decor = w.getDecorView();
                if (enabled) {
                    decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
                } else {
                    decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                }
            }
        });
    }

    public static boolean isImmersive() {
        return immersive;
    }

    public static void setLayoutInCutout(final boolean enabled) {
        if (Build.VERSION.SDK_INT < 28)
            return;

        Utils.runOnUi(() -> {
            Window w = window();
            if (w == null)
                return;

            LayoutParams params = w.getAttributes();
            params.layoutInDisplayCutoutMode = enabled
                ? LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                : LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
            w.setAttributes(params);
        });
    }

    public static void setOrientation(final int orientation) {
        Utils.runOnUi(() -> Utils.activity().setRequestedOrientation(orientation));
    }

    public static int getOrientation() {
        Activity act = Utils.activity();
        return act != null ? act.getRequestedOrientation() : -1;
    }

    public static int getCurrentOrientation() {
        Activity act = Utils.activity();
        return act != null ? act.getResources().getConfiguration().orientation : 0;
    }

    public static void setBrightness(final float value) {
        Utils.runOnUi(() -> {
            Window w = window();
            if (w == null)
                return;

            LayoutParams params = w.getAttributes();
            params.screenBrightness = value < 0 ? LayoutParams.BRIGHTNESS_OVERRIDE_NONE : Math.min(1f, value);
            w.setAttributes(params);
        });
    }

    public static float getBrightness() {
        Window w = window();
        if (w == null)
            return -1f;

        float value = w.getAttributes().screenBrightness;
        if (value >= 0)
            return value;

        try {
            int system = android.provider.Settings.System.getInt(Utils.context().getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
            return system / 255f;
        } catch (Exception e) {
            return -1f;
        }
    }

    public static void setPreferredRefreshRate(final float rate) {
        Utils.runOnUi(() -> {
            Window w = window();
            if (w == null)
                return;

            LayoutParams params = w.getAttributes();
            params.preferredRefreshRate = rate;

            if (Build.VERSION.SDK_INT >= 23 && rate > 0) {
                android.view.Display display = Utils.activity().getWindowManager().getDefaultDisplay();
                android.view.Display.Mode current = display.getMode();
                android.view.Display.Mode best = null;
                for (android.view.Display.Mode mode : display.getSupportedModes()) {
                    if (mode.getPhysicalWidth() != current.getPhysicalWidth() || mode.getPhysicalHeight() != current.getPhysicalHeight())
                        continue;

                    if (best == null || Math.abs(mode.getRefreshRate() - rate) < Math.abs(best.getRefreshRate() - rate))
                        best = mode;
                }
                if (best != null)
                    params.preferredDisplayModeId = best.getModeId();
            } else if (Build.VERSION.SDK_INT >= 23) {
                params.preferredDisplayModeId = 0;
            }

            w.setAttributes(params);
        });
    }

    public static void setSustainedPerformance(final boolean enabled) {
        if (Build.VERSION.SDK_INT < 24)
            return;

        Utils.runOnUi(() -> {
            Window w = window();
            if (w != null)
                w.setSustainedPerformanceMode(enabled);
        });
    }

    public static void setStatusBarColor(final int color) {
        Utils.runOnUi(() -> {
            Window w = window();
            if (w == null)
                return;

            w.addFlags(LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(color);
        });
    }

    public static void setNavigationBarColor(final int color) {
        Utils.runOnUi(() -> {
            Window w = window();
            if (w == null)
                return;

            w.addFlags(LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setNavigationBarColor(color);
        });
    }

    public static void setTitle(final String title) {
        Utils.runOnUi(() -> Utils.activity().setTitle(title));
    }

    public static void minimize() {
        Utils.runOnUi(() -> Utils.activity().moveTaskToBack(true));
    }
}
