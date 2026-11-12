package androidmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import org.haxe.extension.Extension;
import org.json.JSONArray;
import org.json.JSONObject;

public class Utils {

    public static final String LOG_TAG = "AndroidManager";

    public static Activity activity() {
        return Extension.mainActivity;
    }

    public static Context context() {
        if (Extension.mainContext != null)
            return Extension.mainContext;

        return Extension.mainActivity;
    }

    public static String packageName() {
        Context ctx = context();
        return ctx != null ? ctx.getPackageName() : "";
    }

    public static Uri packageUri() {
        return Uri.fromParts("package", packageName(), null);
    }

    public static void runOnUi(final Runnable runnable) {
        Activity act = activity();
        if (act == null || runnable == null)
            return;

        act.runOnUiThread(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
            }
        });
    }

    public static <T> T callOnUi(final java.util.concurrent.Callable<T> callable, T fallback) {
        Activity act = activity();
        if (act == null || callable == null)
            return fallback;

        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            try {
                return callable.call();
            } catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
                return fallback;
            }
        }

        java.util.concurrent.FutureTask<T> task = new java.util.concurrent.FutureTask<>(callable);
        act.runOnUiThread(task);

        try {
            return task.get(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
            return fallback;
        }
    }

    public static boolean startForResult(final Intent intent, final int requestCode) {
        final Activity act = activity();
        if (act == null || intent == null)
            return false;

        act.runOnUiThread(() -> {
            try {
                act.startActivityForResult(intent, requestCode);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
                CallbackManager.dispatchActivityFailure(requestCode, e.toString());
            }
        });
        return true;
    }

    public static boolean startActivity(final Intent intent) {
        final Activity act = activity();
        if (act == null || intent == null)
            return false;

        act.runOnUiThread(() -> {
            try {
                act.startActivity(intent);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
            }
        });
        return true;
    }

    public static Object wrap(Object value) {
        if (value == null)
            return JSONObject.NULL;

        if (value instanceof Bundle)
            return bundleToJson((Bundle) value);

        if (value instanceof Uri)
            return value.toString();

        if (value instanceof Object[]) {
            JSONArray array = new JSONArray();
            for (Object item : (Object[]) value)
                array.put(wrap(item));
            return array;
        }

        Object wrapped = JSONObject.wrap(value);
        return wrapped != null ? wrapped : value.toString();
    }

    public static JSONObject bundleToJson(Bundle bundle) {
        JSONObject json = new JSONObject();
        if (bundle == null)
            return json;

        try {
            for (String key : bundle.keySet())
                json.put(key, wrap(bundle.get(key)));
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
        return json;
    }

    public static JSONObject intentToJson(Intent intent) {
        JSONObject json = new JSONObject();
        if (intent == null)
            return json;

        try {
            json.put("action", intent.getAction() != null ? intent.getAction() : JSONObject.NULL);
            json.put("type", intent.getType() != null ? intent.getType() : JSONObject.NULL);
            json.put("data", intent.getData() != null ? intent.getData().toString() : JSONObject.NULL);
            json.put("flags", intent.getFlags());

            JSONArray uris = new JSONArray();
            if (intent.getData() != null)
                uris.put(intent.getData().toString());

            if (intent.getClipData() != null) {
                for (int i = 0; i < intent.getClipData().getItemCount(); i++) {
                    Uri uri = intent.getClipData().getItemAt(i).getUri();
                    if (uri != null && (intent.getData() == null || !uri.equals(intent.getData())))
                        uris.put(uri.toString());
                }
            }
            json.put("uris", uris);
            json.put("extras", bundleToJson(intent.getExtras()));
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
        return json;
    }

    public static String error(String message) {
        try {
            JSONObject json = new JSONObject();
            json.put("error", message);
            return json.toString();
        } catch (Exception e) {
            return "{}";
        }
    }
}
