package java.androidmanager;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;

public class PermissionManager {

    private static final int REQUEST_CODE = 1024;

    public static boolean hasPermission(String permission) {
        Context ctx = Utils.context();
        if (ctx == null || permission == null)
            return false;

        if (Build.VERSION.SDK_INT >= 23)
            return ctx.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;

        return true;
    }

    public static boolean hasPermissions(String[] permissions) {
        if (Utils.context() == null || permissions == null || permissions.length == 0)
            return false;

        for (String permission : permissions) {
            if (permission != null && !hasPermission(permission))
                return false;
        }
        return true;
    }

    public static void requestPermission(final String permission) {
        if (permission == null)
            return;

        requestPermissionsWithCode(new String[]{ permission }, REQUEST_CODE);
    }

    public static void requestPermissions(final String[] permissions) {
        requestPermissionsWithCode(permissions, REQUEST_CODE);
    }

    public static boolean requestPermissionsWithCode(final String[] permissions, final int requestCode) {
        if (Utils.activity() == null || permissions == null || permissions.length == 0)
            return false;

        if (Build.VERSION.SDK_INT < 23)
            return false;

        final List<String> missing = new ArrayList<>();
        for (String permission : permissions) {
            if (permission != null && !hasPermission(permission) && !missing.contains(permission))
                missing.add(permission);
        }

        if (missing.isEmpty())
            return false;

        Utils.runOnUi(() -> Utils.activity().requestPermissions(missing.toArray(new String[0]), requestCode));
        return true;
    }

    public static boolean shouldShowRationale(String permission) {
        if (Build.VERSION.SDK_INT < 23 || Utils.activity() == null || permission == null)
            return false;

        try {
            return Utils.activity().shouldShowRequestPermissionRationale(permission);
        } catch (Exception e) {
            return false;
        }
    }

    private static PackageInfo packageInfo() throws Exception {
        Context ctx = Utils.context();
        return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_PERMISSIONS);
    }

    public static String getRequestedPermissions() {
        JSONArray array = new JSONArray();
        try {
            PackageInfo info = packageInfo();
            if (info.requestedPermissions != null) {
                for (String permission : info.requestedPermissions)
                    array.put(permission);
            }
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
        return array.toString();
    }

    public static String getGrantedPermissions() {
        JSONArray array = new JSONArray();
        try {
            PackageInfo info = packageInfo();
            if (info.requestedPermissions != null) {
                for (int i = 0; i < info.requestedPermissions.length; i++) {
                    boolean granted = info.requestedPermissionsFlags != null
                        && i < info.requestedPermissionsFlags.length
                        && (info.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0;

                    if (granted)
                        array.put(info.requestedPermissions[i]);
                }
            }
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
        return array.toString();
    }

    public static boolean isExternalStorageManager() {
        if (Build.VERSION.SDK_INT >= 30)
            return Environment.isExternalStorageManager();

        return hasPermission("android.permission.WRITE_EXTERNAL_STORAGE");
    }

    public static boolean requestManageExternalStorage(int requestCode) {
        if (Build.VERSION.SDK_INT >= 30) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Utils.packageUri());

            if (Utils.context() != null && intent.resolveActivity(Utils.context().getPackageManager()) == null)
                intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);

            return Utils.startForResult(intent, requestCode);
        }

        return requestPermissionsWithCode(new String[]{
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        }, requestCode);
    }

    public static boolean canDrawOverlays() {
        if (Build.VERSION.SDK_INT < 23)
            return true;

        return Utils.context() != null && Settings.canDrawOverlays(Utils.context());
    }

    public static boolean requestDrawOverlays(int requestCode) {
        if (Build.VERSION.SDK_INT < 23)
            return false;

        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Utils.packageUri());
        return Utils.startForResult(intent, requestCode);
    }

    public static boolean canWriteSettings() {
        if (Build.VERSION.SDK_INT < 23)
            return true;

        return Utils.context() != null && Settings.System.canWrite(Utils.context());
    }

    public static boolean requestWriteSettings(int requestCode) {
        if (Build.VERSION.SDK_INT < 23)
            return false;

        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Utils.packageUri());
        return Utils.startForResult(intent, requestCode);
    }

    public static boolean canRequestPackageInstalls() {
        if (Build.VERSION.SDK_INT < 26)
            return true;

        return Utils.context() != null && Utils.context().getPackageManager().canRequestPackageInstalls();
    }

    public static boolean requestInstallPackages(int requestCode) {
        if (Build.VERSION.SDK_INT < 26)
            return false;

        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
        intent.setData(Utils.packageUri());
        return Utils.startForResult(intent, requestCode);
    }

    public static boolean openAppSettings(int requestCode) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Utils.packageUri());
        return Utils.startForResult(intent, requestCode);
    }
}
