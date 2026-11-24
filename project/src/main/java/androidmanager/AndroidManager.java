package androidmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.JSONObject;

public final class AndroidManager {

    public static final String VERSION = "1.5.0";
    public static final String TAG = Utils.LOG_TAG;

    public static final int LOG_VERBOSE = Log.VERBOSE;
    public static final int LOG_DEBUG = Log.DEBUG;
    public static final int LOG_INFO = Log.INFO;
    public static final int LOG_WARN = Log.WARN;
    public static final int LOG_ERROR = Log.ERROR;

    public interface Listener {
        default void onLifecycle(String state) {}
        default void onActivityResult(int requestCode, int resultCode, Intent data) {}
        default void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {}
        default boolean onBackPressed() { return false; }
        default void onLowMemory() {}
        default void onTrimMemory(int level) {}
        default void onNewIntent(Intent intent) {}
    }

    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static boolean logging = true;

    private AndroidManager() {}

    public static String getVersion() {
        return VERSION;
    }

    public static Activity getActivity() {
        return Utils.activity();
    }

    public static Context getContext() {
        return Utils.context();
    }

    public static boolean isReady() {
        return Utils.activity() != null;
    }

    public static String getPackageName() {
        return Utils.packageName();
    }

    public static int getSdkInt() {
        return Build.VERSION.SDK_INT;
    }

    public static boolean isAtLeast(int sdk) {
        return Build.VERSION.SDK_INT >= sdk;
    }

    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static boolean isPaused() {
        return CallbackManager.isPaused();
    }

    public static void runOnUiThread(Runnable runnable) {
        Utils.runOnUi(runnable);
    }

    public static <T> T callOnUiThread(Callable<T> callable, T fallback) {
        return Utils.callOnUi(callable, fallback);
    }

    public static void postDelayed(final Runnable runnable, long delayMs) {
        if (runnable == null)
            return;

        mainHandler.postDelayed(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }, Math.max(0, delayMs));
    }

    public static void runInBackground(final Runnable runnable) {
        if (runnable == null)
            return;

        Thread thread = new Thread(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }, "AndroidManager-Worker");
        thread.setDaemon(true);
        thread.start();
    }

    public static void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener))
            listeners.add(listener);
    }

    public static void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public static void clearListeners() {
        listeners.clear();
    }

    public static void setLogging(boolean enabled) {
        logging = enabled;
    }

    public static boolean isLogging() {
        return logging;
    }

    public static void log(int priority, String message) {
        if (logging)
            Log.println(priority, TAG, message != null ? message : "null");
    }

    public static void d(String message) {
        log(LOG_DEBUG, message);
    }

    public static void i(String message) {
        log(LOG_INFO, message);
    }

    public static void w(String message) {
        log(LOG_WARN, message);
    }

    public static void e(String message) {
        log(LOG_ERROR, message);
    }

    public static String getInfo() {
        try {
            JSONObject json = new JSONObject();
            json.put("version", VERSION);
            json.put("ready", isReady());
            json.put("paused", isPaused());
            json.put("packageName", getPackageName());
            json.put("sdkInt", getSdkInt());
            json.put("build", new JSONObject(BuildInfo.toJson()));
            return json.toString();
        } catch (Exception e) {
            return Utils.error(e.toString());
        }
    }

    static void notifyLifecycle(String state) {
        for (Listener listener : listeners) {
            try {
                listener.onLifecycle(state);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    static void notifyActivityResult(int requestCode, int resultCode, Intent data) {
        for (Listener listener : listeners) {
            try {
                listener.onActivityResult(requestCode, resultCode, data);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    static void notifyPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        for (Listener listener : listeners) {
            try {
                listener.onRequestPermissionsResult(requestCode, permissions, grantResults);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    static boolean notifyBackPressed() {
        boolean handled = false;
        for (Listener listener : listeners) {
            try {
                handled |= listener.onBackPressed();
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
        return handled;
    }

    static void notifyLowMemory() {
        for (Listener listener : listeners) {
            try {
                listener.onLowMemory();
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    static void notifyTrimMemory(int level) {
        for (Listener listener : listeners) {
            try {
                listener.onTrimMemory(level);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    static void notifyNewIntent(Intent intent) {
        for (Listener listener : listeners) {
            try {
                listener.onNewIntent(intent);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }
}
