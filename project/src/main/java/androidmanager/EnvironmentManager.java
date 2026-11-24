package androidmanager;

import android.os.Build;
import android.os.Environment;
import java.io.File;

public final class EnvironmentManager {

    private EnvironmentManager() {}

    private static String path(File file) {
        return file != null ? file.getAbsolutePath() : "";
    }

    public static String getDirectoryName(String key) {
        if (key == null)
            return "";

        switch (key) {
            case "DOWNLOADS": return Environment.DIRECTORY_DOWNLOADS;
            case "MUSIC": return Environment.DIRECTORY_MUSIC;
            case "PICTURES": return Environment.DIRECTORY_PICTURES;
            case "DOCUMENTS": return Build.VERSION.SDK_INT >= 19 ? Environment.DIRECTORY_DOCUMENTS : "Documents";
            case "MOVIES": return Environment.DIRECTORY_MOVIES;
            case "DCIM": return Environment.DIRECTORY_DCIM;
            case "ALARMS": return Environment.DIRECTORY_ALARMS;
            case "RINGTONES": return Environment.DIRECTORY_RINGTONES;
            case "NOTIFICATIONS": return Environment.DIRECTORY_NOTIFICATIONS;
            case "PODCASTS": return Environment.DIRECTORY_PODCASTS;
            case "AUDIOBOOKS": return Build.VERSION.SDK_INT >= 29 ? Environment.DIRECTORY_AUDIOBOOKS : "Audiobooks";
            case "SCREENSHOTS": return Build.VERSION.SDK_INT >= 29 ? Environment.DIRECTORY_SCREENSHOTS : "Screenshots";
            case "RECORDINGS": return Build.VERSION.SDK_INT >= 31 ? Environment.DIRECTORY_RECORDINGS : "Recordings";
            default: return "";
        }
    }

    @SuppressWarnings("deprecation")
    public static String getExternalStorageDirectory() {
        return path(Environment.getExternalStorageDirectory());
    }

    @SuppressWarnings("deprecation")
    public static String getExternalStoragePublicDirectory(String type) {
        if (type == null || type.isEmpty())
            return getExternalStorageDirectory();
        return path(Environment.getExternalStoragePublicDirectory(type));
    }

    public static String getDataDirectory() {
        return path(Environment.getDataDirectory());
    }

    public static String getRootDirectory() {
        return path(Environment.getRootDirectory());
    }

    public static String getDownloadCacheDirectory() {
        return path(Environment.getDownloadCacheDirectory());
    }

    public static String getStorageDirectory() {
        if (Build.VERSION.SDK_INT >= 30)
            return path(Environment.getStorageDirectory());
        return "/storage";
    }

    public static String getExternalStorageState() {
        String state = Environment.getExternalStorageState();
        return state != null ? state : Environment.MEDIA_UNKNOWN;
    }

    public static boolean isExternalStorageMounted() {
        return Environment.MEDIA_MOUNTED.equals(getExternalStorageState());
    }

    public static boolean isExternalStorageReadOnly() {
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(getExternalStorageState());
    }

    public static boolean isExternalStorageManager() {
        if (Build.VERSION.SDK_INT >= 30)
            return Environment.isExternalStorageManager();
        return true;
    }

    public static boolean isExternalStorageRemovable() {
        try {
            return Environment.isExternalStorageRemovable();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isExternalStorageEmulated() {
        try {
            return Environment.isExternalStorageEmulated();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isExternalStorageLegacy() {
        if (Build.VERSION.SDK_INT >= 29)
            return Environment.isExternalStorageLegacy();
        return true;
    }
}
