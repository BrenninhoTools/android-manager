package java.androidmanager;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import org.haxe.extension.Extension;
import org.haxe.lime.HaxeObject;
import org.json.JSONArray;
import org.json.JSONObject;

public class CallbackManager extends Extension {

    private static HaxeObject listener = null;
    private static boolean interceptBack = false;
    private static boolean paused = false;

    public static void setListener(HaxeObject object) {
        listener = object;
    }

    public static void setInterceptBack(boolean value) {
        interceptBack = value;
    }

    public static boolean isInterceptingBack() {
        return interceptBack;
    }

    public static boolean isPaused() {
        return paused;
    }

    public static void dispatch(final String method, final Object... args) {
        final HaxeObject target = listener;
        if (target == null)
            return;

        try {
            target.call(method, args);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, "Callback " + method + " failed: " + e);
        }
    }

    public static void dispatchActivityFailure(int requestCode, String message) {
        try {
            JSONObject json = new JSONObject();
            json.put("requestCode", requestCode);
            json.put("resultCode", 0);
            json.put("data", JSONObject.NULL);
            json.put("error", message);
            dispatch("onActivityResult", json.toString());
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            JSONObject json = new JSONObject();
            json.put("requestCode", requestCode);
            json.put("resultCode", resultCode);
            json.put("data", data != null ? Utils.intentToJson(data) : JSONObject.NULL);
            dispatch("onActivityResult", json.toString());
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
        return true;
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        try {
            JSONObject json = new JSONObject();
            JSONArray granted = new JSONArray();
            JSONArray denied = new JSONArray();
            JSONArray permanentlyDenied = new JSONArray();

            if (permissions != null && grantResults != null) {
                for (int i = 0; i < permissions.length && i < grantResults.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        granted.put(permissions[i]);
                    } else {
                        denied.put(permissions[i]);
                        if (!PermissionManager.shouldShowRationale(permissions[i]))
                            permanentlyDenied.put(permissions[i]);
                    }
                }
            }

            json.put("requestCode", requestCode);
            json.put("granted", granted);
            json.put("denied", denied);
            json.put("permanentlyDenied", permanentlyDenied);
            dispatch("onRequestPermissionsResult", json.toString());
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
        return true;
    }

    @Override
    public boolean onBackPressed() {
        dispatch("onBackPressed");
        return !interceptBack;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        dispatch("onLifecycle", "create");
    }

    @Override
    public void onStart() {
        dispatch("onLifecycle", "start");
    }

    @Override
    public void onRestart() {
        dispatch("onLifecycle", "restart");
    }

    @Override
    public void onResume() {
        paused = false;
        dispatch("onLifecycle", "resume");
    }

    @Override
    public void onPause() {
        paused = true;
        dispatch("onLifecycle", "pause");
    }

    @Override
    public void onStop() {
        dispatch("onLifecycle", "stop");
    }

    @Override
    public void onDestroy() {
        dispatch("onLifecycle", "destroy");
    }

    @Override
    public void onLowMemory() {
        dispatch("onLowMemory");
    }

    @Override
    public void onTrimMemory(int level) {
        dispatch("onTrimMemory", level);
    }

    @Override
    public void onNewIntent(Intent intent) {
        dispatch("onNewIntent", Utils.intentToJson(intent).toString());
    }
}
