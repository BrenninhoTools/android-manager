package androidmanager;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager.LayoutParams;
import android.graphics.Insets;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceView;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import org.haxe.extension.Extension;
import org.haxe.lime.HaxeObject;
import org.json.JSONArray;
import org.json.JSONObject;

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
        if (enabled)
            ensureFocusListener();
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

    private static HaxeObject listener = null;
    private static ViewTreeObserver.OnGlobalLayoutListener layoutListener = null;
    private static ViewTreeObserver.OnWindowFocusChangeListener focusListener = null;
    private static boolean autoReapplyImmersive = true;
    private static String lastInfo = null;
    private static boolean lastKeyboard = false;

    private static View decor() {
        Window w = window();
        return w != null ? w.getDecorView() : null;
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

    public static void setAutoReapplyImmersive(boolean enabled) {
        autoReapplyImmersive = enabled;
    }

    private static void ensureFocusListener() {
        Utils.runOnUi(() -> {
            View view = decor();
            if (view == null || focusListener != null)
                return;

            focusListener = new ViewTreeObserver.OnWindowFocusChangeListener() {
                @Override
                public void onWindowFocusChanged(boolean hasFocus) {
                    if (hasFocus && immersive && autoReapplyImmersive)
                        setImmersive(true);

                    dispatch("onFocusChanged", hasFocus);
                }
            };
            view.getViewTreeObserver().addOnWindowFocusChangeListener(focusListener);
        });
    }

    private static JSONObject insetsJson(int top, int bottom, int left, int right) throws Exception {
        JSONObject json = new JSONObject();
        json.put("top", top);
        json.put("bottom", bottom);
        json.put("left", left);
        json.put("right", right);
        return json;
    }

    private static JSONObject insetsOf(WindowInsets insets, int type) throws Exception {
        Insets value = insets.getInsets(type);
        return insetsJson(value.top, value.bottom, value.left, value.right);
    }

    @SuppressWarnings("deprecation")
    private static JSONObject buildInfo() throws Exception {
        JSONObject json = new JSONObject();
        Activity act = Utils.activity();
        View view = decor();
        if (act == null || view == null)
            return json;

        json.put("width", view.getWidth());
        json.put("height", view.getHeight());
        json.put("focused", view.hasWindowFocus());
        json.put("orientation", act.getResources().getConfiguration().orientation);
        json.put("rotation", act.getWindowManager().getDefaultDisplay().getRotation());
        json.put("density", (double) act.getResources().getDisplayMetrics().density);
        json.put("immersive", immersive);
        json.put("multiWindow", Build.VERSION.SDK_INT >= 24 && act.isInMultiWindowMode());
        json.put("pictureInPicture", Build.VERSION.SDK_INT >= 24 && act.isInPictureInPictureMode());

        JSONObject zero = insetsJson(0, 0, 0, 0);
        WindowInsets insets = view.getRootWindowInsets();
        boolean keyboard = false;
        int keyboardHeight = 0;

        if (insets != null && Build.VERSION.SDK_INT >= 30) {
            json.put("statusBars", insetsOf(insets, WindowInsets.Type.statusBars()));
            json.put("navigationBars", insetsOf(insets, WindowInsets.Type.navigationBars()));
            json.put("displayCutout", insetsOf(insets, WindowInsets.Type.displayCutout()));
            json.put("systemGestures", insetsOf(insets, WindowInsets.Type.systemGestures()));
            json.put("ime", insetsOf(insets, WindowInsets.Type.ime()));
            keyboard = insets.isVisible(WindowInsets.Type.ime());
            keyboardHeight = insets.getInsets(WindowInsets.Type.ime()).bottom;
            json.put("statusBarVisible", insets.isVisible(WindowInsets.Type.statusBars()));
            json.put("navigationBarVisible", insets.isVisible(WindowInsets.Type.navigationBars()));
        } else if (insets != null && Build.VERSION.SDK_INT >= 23) {
            JSONObject system = insetsJson(insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetRight());
            JSONObject nav = insetsJson(0, insets.getStableInsetBottom() > 0 ? Math.min(insets.getSystemWindowInsetBottom(), insets.getStableInsetBottom()) : 0, 0, 0);
            json.put("statusBars", system);
            json.put("navigationBars", nav);
            JSONObject cutout = zero;
            if (Build.VERSION.SDK_INT >= 28 && insets.getDisplayCutout() != null) {
                android.view.DisplayCutout dc = insets.getDisplayCutout();
                cutout = insetsJson(dc.getSafeInsetTop(), dc.getSafeInsetBottom(), dc.getSafeInsetLeft(), dc.getSafeInsetRight());
            }
            json.put("displayCutout", cutout);
            json.put("systemGestures", zero);
            int imeHeight = Math.max(0, insets.getSystemWindowInsetBottom() - insets.getStableInsetBottom());
            keyboard = imeHeight > view.getHeight() * 0.15;
            keyboardHeight = keyboard ? imeHeight : 0;
            json.put("ime", insetsJson(0, keyboardHeight, 0, 0));
            json.put("statusBarVisible", insets.getSystemWindowInsetTop() > 0);
            json.put("navigationBarVisible", insets.getStableInsetBottom() > 0);
        } else {
            json.put("statusBars", zero);
            json.put("navigationBars", zero);
            json.put("displayCutout", zero);
            json.put("systemGestures", zero);
            json.put("ime", zero);
            json.put("statusBarVisible", !immersive);
            json.put("navigationBarVisible", !immersive);
        }

        json.put("keyboardVisible", keyboard);
        json.put("keyboardHeight", keyboardHeight);
        return json;
    }

    public static String getWindowInfo() {
        String result = Utils.callOnUi(() -> buildInfo().toString(), "{}");
        return result != null ? result : "{}";
    }

    public static void startListening(final HaxeObject callback) {
        listener = callback;
        ensureFocusListener();

        Utils.runOnUi(() -> {
            View view = decor();
            if (view == null || layoutListener != null)
                return;

            layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    try {
                        JSONObject info = buildInfo();
                        String content = info.toString();
                        if (content.equals(lastInfo))
                            return;

                        lastInfo = content;
                        dispatch("onWindowInfo", content);

                        boolean keyboard = info.optBoolean("keyboardVisible", false);
                        if (keyboard != lastKeyboard) {
                            lastKeyboard = keyboard;
                            dispatch("onKeyboardChanged", keyboard, info.optInt("keyboardHeight", 0));
                        }
                    } catch (Exception e) {
                        Log.e("AndroidManager", e.toString());
                    }
                }
            };
            view.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
        });
    }

    public static void stopListening() {
        Utils.runOnUi(() -> {
            View view = decor();
            if (view != null && layoutListener != null)
                view.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);

            layoutListener = null;
            lastInfo = null;
        });
    }

    @SuppressWarnings("deprecation")
    public static void setSystemBarsVisible(final boolean statusBar, final boolean navigationBar) {
        immersive = !statusBar && !navigationBar;
        if (immersive)
            ensureFocusListener();

        Utils.runOnUi(() -> {
            Window w = window();
            if (w == null)
                return;

            if (Build.VERSION.SDK_INT >= 30) {
                WindowInsetsController controller = w.getInsetsController();
                if (controller == null)
                    return;

                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                if (statusBar)
                    controller.show(WindowInsets.Type.statusBars());
                else
                    controller.hide(WindowInsets.Type.statusBars());

                if (navigationBar)
                    controller.show(WindowInsets.Type.navigationBars());
                else
                    controller.hide(WindowInsets.Type.navigationBars());
            } else {
                int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                if (!statusBar)
                    flags |= View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                if (!navigationBar)
                    flags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                if (!statusBar || !navigationBar)
                    flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                w.getDecorView().setSystemUiVisibility(flags);
            }
        });
    }

    @SuppressWarnings("deprecation")
    public static void setLightSystemBars(final boolean lightStatusBar, final boolean lightNavigationBar) {
        Utils.runOnUi(() -> {
            Window w = window();
            if (w == null)
                return;

            if (Build.VERSION.SDK_INT >= 30) {
                WindowInsetsController controller = w.getInsetsController();
                if (controller == null)
                    return;

                controller.setSystemBarsAppearance(lightStatusBar ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                controller.setSystemBarsAppearance(lightNavigationBar ? WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS : 0,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            } else if (Build.VERSION.SDK_INT >= 23) {
                View view = w.getDecorView();
                int flags = view.getSystemUiVisibility();
                flags = lightStatusBar ? flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= 26)
                    flags = lightNavigationBar ? flags | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : flags & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                view.setSystemUiVisibility(flags);
            }
        });
    }

    @SuppressWarnings("deprecation")
    public static void setEdgeToEdge(final boolean enabled) {
        Utils.runOnUi(() -> {
            Window w = window();
            if (w == null)
                return;

            if (Build.VERSION.SDK_INT >= 30) {
                w.setDecorFitsSystemWindows(!enabled);
            } else {
                View view = w.getDecorView();
                int flags = view.getSystemUiVisibility();
                int layout = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                view.setSystemUiVisibility(enabled ? flags | layout : flags & ~layout);
            }

            if (enabled) {
                w.setStatusBarColor(0);
                w.setNavigationBarColor(0);
            }
        });
    }

    @SuppressWarnings("deprecation")
    public static void setShowWhenLocked(final boolean enabled) {
        Utils.runOnUi(() -> {
            Activity act = Utils.activity();
            if (Build.VERSION.SDK_INT >= 27)
                act.setShowWhenLocked(enabled);
            else
                setFlag(LayoutParams.FLAG_SHOW_WHEN_LOCKED, enabled);
        });
    }

    @SuppressWarnings("deprecation")
    public static void setTurnScreenOn(final boolean enabled) {
        Utils.runOnUi(() -> {
            Activity act = Utils.activity();
            if (Build.VERSION.SDK_INT >= 27)
                act.setTurnScreenOn(enabled);
            else
                setFlag(LayoutParams.FLAG_TURN_SCREEN_ON, enabled);
        });
    }

    public static void lockCurrentOrientation() {
        setOrientation(14);
    }

    @SuppressWarnings("deprecation")
    public static String getDisplayModes() {
        JSONArray array = new JSONArray();
        Activity act = Utils.activity();
        if (act == null || Build.VERSION.SDK_INT < 23)
            return array.toString();

        try {
            Display display = act.getWindowManager().getDefaultDisplay();
            int current = display.getMode().getModeId();
            for (Display.Mode mode : display.getSupportedModes()) {
                JSONObject json = new JSONObject();
                json.put("id", mode.getModeId());
                json.put("width", mode.getPhysicalWidth());
                json.put("height", mode.getPhysicalHeight());
                json.put("refreshRate", Math.round(mode.getRefreshRate() * 100.0) / 100.0);
                json.put("current", mode.getModeId() == current);
                array.put(json);
            }
        } catch (Exception e) {
            Log.e("AndroidManager", e.toString());
        }
        return array.toString();
    }

    public static void setDisplayMode(final int modeId) {
        if (Build.VERSION.SDK_INT < 23)
            return;

        Utils.runOnUi(() -> {
            Window w = window();
            if (w == null)
                return;

            LayoutParams params = w.getAttributes();
            params.preferredDisplayModeId = modeId;
            w.setAttributes(params);
        });
    }

    @SuppressWarnings("deprecation")
    public static float getMaxRefreshRate() {
        Activity act = Utils.activity();
        if (act == null)
            return 60f;

        Display display = act.getWindowManager().getDefaultDisplay();
        float max = display.getRefreshRate();
        if (Build.VERSION.SDK_INT >= 23) {
            for (Display.Mode mode : display.getSupportedModes()) {
                if (mode.getPhysicalWidth() == display.getMode().getPhysicalWidth() && mode.getRefreshRate() > max)
                    max = mode.getRefreshRate();
            }
        }
        return max;
    }

    public static void setHighRefreshRate(boolean enabled) {
        setPreferredRefreshRate(enabled ? getMaxRefreshRate() : 0f);
        setSurfaceFrameRate(enabled ? getMaxRefreshRate() : 0f);
    }

    public static void setSurfaceFrameRate(final float rate) {
        if (Build.VERSION.SDK_INT < 30)
            return;

        Utils.runOnUi(() -> {
            View view = Extension.mainView;
            if (!(view instanceof SurfaceView))
                return;

            android.view.Surface surface = ((SurfaceView) view).getHolder().getSurface();
            if (surface != null && surface.isValid())
                surface.setFrameRate(Math.max(0f, rate), android.view.Surface.FRAME_RATE_COMPATIBILITY_DEFAULT);
        });
    }

    public static void setColorMode(final int mode) {
        if (Build.VERSION.SDK_INT < 26)
            return;

        Utils.runOnUi(() -> {
            Window w = window();
            if (w != null)
                w.setColorMode(mode);
        });
    }

    public static boolean isWideColorGamut() {
        Activity act = Utils.activity();
        return act != null && Build.VERSION.SDK_INT >= 26 && act.getResources().getConfiguration().isScreenWideColorGamut();
    }

    public static boolean isHdr() {
        Activity act = Utils.activity();
        return act != null && Build.VERSION.SDK_INT >= 26 && act.getResources().getConfiguration().isScreenHdr();
    }

    public static void setDimAmount(final float amount) {
        Utils.runOnUi(() -> {
            Window w = window();
            if (w == null)
                return;

            if (amount > 0)
                w.addFlags(LayoutParams.FLAG_DIM_BEHIND);
            else
                w.clearFlags(LayoutParams.FLAG_DIM_BEHIND);
            w.setDimAmount(Math.max(0f, Math.min(1f, amount)));
        });
    }

    public static void showKeyboard() {
        Utils.runOnUi(() -> {
            View view = Extension.mainView != null ? Extension.mainView : decor();
            if (view == null)
                return;

            view.requestFocus();
            InputMethodManager imm = (InputMethodManager) Utils.activity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null)
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    public static void hideKeyboard() {
        Utils.runOnUi(() -> {
            View view = Extension.mainView != null ? Extension.mainView : decor();
            InputMethodManager imm = (InputMethodManager) Utils.activity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null && view != null)
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        });
    }
}
