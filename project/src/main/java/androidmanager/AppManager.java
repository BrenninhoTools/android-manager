package androidmanager;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.util.Log;
import org.json.JSONObject;

public class AppManager {

    public static String getAppInfo() {
        JSONObject json = new JSONObject();
        try {
            Context ctx = Utils.context();
            PackageManager pm = ctx.getPackageManager();
            PackageInfo info = pm.getPackageInfo(ctx.getPackageName(), 0);
            ApplicationInfo app = ctx.getApplicationInfo();

            long versionCode = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;

            json.put("packageName", ctx.getPackageName());
            json.put("label", String.valueOf(pm.getApplicationLabel(app)));
            json.put("versionName", info.versionName != null ? info.versionName : "");
            json.put("versionCode", (double) versionCode);
            json.put("firstInstallTime", (double) info.firstInstallTime);
            json.put("lastUpdateTime", (double) info.lastUpdateTime);
            json.put("targetSdk", app.targetSdkVersion);
            json.put("minSdk", Build.VERSION.SDK_INT >= 24 ? app.minSdkVersion : -1);
            json.put("debuggable", (app.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
            json.put("sourceDir", app.sourceDir);
            json.put("nativeLibraryDir", app.nativeLibraryDir);
            json.put("installer", getInstallerPackage());
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
        return json.toString();
    }

    @SuppressWarnings("deprecation")
    public static String getInstallerPackage() {
        try {
            Context ctx = Utils.context();
            PackageManager pm = ctx.getPackageManager();
            String installer;
            if (Build.VERSION.SDK_INT >= 30)
                installer = pm.getInstallSourceInfo(ctx.getPackageName()).getInstallingPackageName();
            else
                installer = pm.getInstallerPackageName(ctx.getPackageName());

            return installer != null ? installer : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static String getLaunchIntent() {
        Activity act = Utils.activity();
        return act != null ? Utils.intentToJson(act.getIntent()).toString() : "{}";
    }

    public static boolean isPackageInstalled(String packageName) {
        try {
            Utils.context().getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean launchPackage(String packageName, int requestCode) {
        try {
            Intent intent = Utils.context().getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent == null)
                return false;

            return requestCode >= 0 ? Utils.startForResult(intent, requestCode) : Utils.startActivity(intent);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return false;
        }
    }

    public static boolean openURL(String url) {
        if (url == null || url.isEmpty())
            return false;

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return Utils.startActivity(intent);
    }

    public static boolean openStorePage(String packageName) {
        String target = (packageName == null || packageName.isEmpty()) ? Utils.packageName() : packageName;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + target));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (intent.resolveActivity(Utils.context().getPackageManager()) == null)
            intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + target));

        return Utils.startActivity(intent);
    }

    public static boolean shareText(String text, String subject, String chooserTitle) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        if (subject != null && !subject.isEmpty())
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);

        return Utils.startActivity(Intent.createChooser(intent, chooserTitle));
    }

    public static boolean shareUri(String uri, String mimeType, String chooserTitle) {
        if (uri == null || uri.isEmpty())
            return false;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType == null || mimeType.isEmpty() ? "*/*" : mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return Utils.startActivity(Intent.createChooser(intent, chooserTitle));
    }

    public static boolean sendEmail(String to, String subject, String body) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{ to });
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        return Utils.startActivity(intent);
    }

    public static boolean startIntent(String action, String data, String mimeType, String packageName, int requestCode) {
        try {
            Intent intent = new Intent(action);
            Uri uri = (data == null || data.isEmpty()) ? null : Uri.parse(data);
            String type = (mimeType == null || mimeType.isEmpty()) ? null : mimeType;

            if (uri != null && type != null)
                intent.setDataAndType(uri, type);
            else if (uri != null)
                intent.setData(uri);
            else if (type != null)
                intent.setType(type);

            if (packageName != null && !packageName.isEmpty())
                intent.setPackage(packageName);

            return requestCode >= 0 ? Utils.startForResult(intent, requestCode) : Utils.startActivity(intent);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return false;
        }
    }

    public static void restart() {
        try {
            Context ctx = Utils.context();
            Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
            if (intent == null)
                return;

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            int flags = PendingIntent.FLAG_CANCEL_CURRENT;
            if (Build.VERSION.SDK_INT >= 23)
                flags |= PendingIntent.FLAG_IMMUTABLE;

            PendingIntent pending = PendingIntent.getActivity(ctx, 0x414D, intent, flags);
            AlarmManager alarm = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            alarm.set(AlarmManager.RTC, System.currentTimeMillis() + 300, pending);

            exit();
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    public static void exit() {
        final Activity act = Utils.activity();
        if (act == null) {
            Process.killProcess(Process.myPid());
            System.exit(0);
            return;
        }

        act.runOnUiThread(() -> {
            act.finishAffinity();
            Process.killProcess(Process.myPid());
            System.exit(0);
        });
    }
}
