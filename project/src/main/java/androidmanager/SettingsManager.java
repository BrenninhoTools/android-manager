package androidmanager;

import android.content.ContentResolver;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

public class SettingsManager {

    private static ContentResolver resolver() {
        return Utils.context() != null ? Utils.context().getContentResolver() : null;
    }

    public static String getString(int table, String name) {
        ContentResolver cr = resolver();
        if (cr == null || name == null)
            return null;

        try {
            switch (table) {
                case 1:
                    return Settings.Secure.getString(cr, name);
                case 2:
                    return Settings.Global.getString(cr, name);
                default:
                    return Settings.System.getString(cr, name);
            }
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return null;
        }
    }

    public static int getInt(int table, String name, int fallback) {
        ContentResolver cr = resolver();
        if (cr == null || name == null)
            return fallback;

        try {
            switch (table) {
                case 1:
                    return Settings.Secure.getInt(cr, name, fallback);
                case 2:
                    return Settings.Global.getInt(cr, name, fallback);
                default:
                    return Settings.System.getInt(cr, name, fallback);
            }
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return fallback;
        }
    }

    public static float getFloat(int table, String name, float fallback) {
        ContentResolver cr = resolver();
        if (cr == null || name == null)
            return fallback;

        try {
            switch (table) {
                case 1:
                    return Settings.Secure.getFloat(cr, name, fallback);
                case 2:
                    return Settings.Global.getFloat(cr, name, fallback);
                default:
                    return Settings.System.getFloat(cr, name, fallback);
            }
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return fallback;
        }
    }

    public static boolean putSystemInt(String name, int value) {
        ContentResolver cr = resolver();
        if (cr == null || name == null || !PermissionManager.canWriteSettings())
            return false;

        try {
            return Settings.System.putInt(cr, name, value);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return false;
        }
    }

    public static boolean open(String action, int requestCode, boolean withPackage) {
        try {
            Intent intent = new Intent(action);
            if (withPackage)
                intent.setData(Utils.packageUri());

            if (Utils.context() != null && intent.resolveActivity(Utils.context().getPackageManager()) == null)
                intent = new Intent(Settings.ACTION_SETTINGS);

            return Utils.startForResult(intent, requestCode);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return false;
        }
    }

    public static boolean openAppNotifications(int requestCode) {
        Intent intent;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, Utils.packageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Utils.packageUri());
        }
        return Utils.startForResult(intent, requestCode);
    }
}
