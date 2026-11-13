package androidmanager;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.haxe.lime.HaxeObject;
import org.json.JSONArray;
import org.json.JSONObject;

public class StorageTools {

    private static Context context() {
        return Utils.context();
    }

    private static JSONObject space(File dir) throws Exception {
        JSONObject json = new JSONObject();
        try {
            StatFs stat = new StatFs(dir.getAbsolutePath());
            json.put("total", (double) stat.getTotalBytes());
            json.put("free", (double) stat.getFreeBytes());
            json.put("available", (double) stat.getAvailableBytes());
        } catch (Exception e) {
            json.put("total", -1);
            json.put("free", -1);
            json.put("available", -1);
        }
        return json;
    }

    public static String getSpace(String path) {
        try {
            File file = new File(path == null || path.isEmpty() ? Environment.getDataDirectory().getAbsolutePath() : path);
            while (file != null && !file.exists())
                file = file.getParentFile();

            JSONObject json = space(file != null ? file : Environment.getDataDirectory());
            json.put("path", file != null ? file.getAbsolutePath() : "");
            return json.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    @SuppressWarnings("deprecation")
    private static String volumePath(StorageVolume volume) {
        try {
            if (Build.VERSION.SDK_INT >= 30 && volume.getDirectory() != null)
                return volume.getDirectory().getAbsolutePath();

            Object value = StorageVolume.class.getMethod("getPath").invoke(volume);
            return value != null ? value.toString() : null;
        } catch (Throwable e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    public static String getVolumes() {
        JSONArray array = new JSONArray();
        Context ctx = context();
        if (ctx == null)
            return array.toString();

        try {
            if (Build.VERSION.SDK_INT >= 24) {
                StorageManager sm = (StorageManager) ctx.getSystemService(Context.STORAGE_SERVICE);
                for (StorageVolume volume : sm.getStorageVolumes()) {
                    JSONObject json = new JSONObject();
                    String path = volumePath(volume);
                    json.put("path", path != null ? path : "");
                    json.put("description", volume.getDescription(ctx));
                    json.put("primary", volume.isPrimary());
                    json.put("removable", volume.isRemovable());
                    json.put("emulated", volume.isEmulated());
                    json.put("state", volume.getState());
                    json.put("uuid", volume.getUuid() != null ? volume.getUuid() : "");
                    json.put("space", path != null ? space(new File(path)) : space(Environment.getExternalStorageDirectory()));
                    array.put(json);
                }
            } else {
                File root = Environment.getExternalStorageDirectory();
                JSONObject json = new JSONObject();
                json.put("path", root.getAbsolutePath());
                json.put("description", "Internal storage");
                json.put("primary", true);
                json.put("removable", Environment.isExternalStorageRemovable());
                json.put("emulated", Environment.isExternalStorageEmulated());
                json.put("state", Environment.getExternalStorageState());
                json.put("uuid", "");
                json.put("space", space(root));
                array.put(json);
            }

            File[] dirs = ctx.getExternalFilesDirs(null);
            JSONArray appDirs = new JSONArray();
            if (dirs != null) {
                for (File dir : dirs) {
                    if (dir != null)
                        appDirs.put(dir.getAbsolutePath());
                }
            }

            for (int i = 0; i < array.length(); i++) {
                JSONObject volume = array.getJSONObject(i);
                String path = volume.getString("path");
                String appDir = "";
                for (int j = 0; j < appDirs.length(); j++) {
                    if (!path.isEmpty() && appDirs.getString(j).startsWith(path)) {
                        appDir = appDirs.getString(j);
                        break;
                    }
                }
                volume.put("appDirectory", appDir);
            }
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
        return array.toString();
    }

    @SuppressWarnings("deprecation")
    public static String getMediaDirectory() {
        Context ctx = context();
        if (ctx == null)
            return null;

        File[] dirs = ctx.getExternalMediaDirs();
        if (dirs != null && dirs.length > 0 && dirs[0] != null) {
            if (!dirs[0].exists())
                dirs[0].mkdirs();
            return dirs[0].getAbsolutePath();
        }
        return null;
    }

    public static String getExternalCacheDirectory() {
        Context ctx = context();
        File dir = ctx != null ? ctx.getExternalCacheDir() : null;
        return dir != null ? dir.getAbsolutePath() : null;
    }

    @SuppressWarnings("deprecation")
    public static String getRootDirectory() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    @SuppressWarnings("deprecation")
    public static String getPublicDirectory(String type) {
        return Environment.getExternalStoragePublicDirectory(type).getAbsolutePath();
    }

    public static String getState(String path) {
        try {
            if (path == null || path.isEmpty())
                return Environment.getExternalStorageState();

            return Environment.getExternalStorageState(new File(path));
        } catch (Exception e) {
            return Environment.MEDIA_UNKNOWN;
        }
    }

    @SuppressWarnings("deprecation")
    public static boolean isLegacyStorage() {
        if (Build.VERSION.SDK_INT < 29)
            return true;

        return Environment.isExternalStorageLegacy();
    }

    public static boolean isWritable(String path) {
        if (path == null || path.isEmpty())
            return false;

        File dir = new File(path);
        boolean created = false;
        if (!dir.exists())
            created = dir.mkdirs();

        if (!dir.isDirectory())
            return false;

        File probe = new File(dir, ".androidmanager_" + System.nanoTime());
        try {
            boolean ok = probe.createNewFile();
            probe.delete();
            return ok;
        } catch (Exception e) {
            return false;
        } finally {
            if (created)
                dir.delete();
        }
    }

    public static boolean createNoMedia(String path) {
        try {
            File dir = new File(path);
            if (!dir.exists())
                dir.mkdirs();

            File file = new File(dir, ".nomedia");
            return file.exists() || file.createNewFile();
        } catch (Exception e) {
            return false;
        }
    }

    public static String list(String path, boolean recursive, boolean includeHidden) {
        JSONArray array = new JSONArray();
        try {
            listInto(new File(path), new File(path), recursive, includeHidden, array);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
        return array.toString();
    }

    private static void listInto(File root, File dir, boolean recursive, boolean includeHidden, JSONArray array) throws Exception {
        File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (!includeHidden && file.getName().startsWith("."))
                continue;

            JSONObject json = new JSONObject();
            json.put("name", file.getName());
            json.put("path", file.getAbsolutePath());
            json.put("relative", root.toURI().relativize(file.toURI()).getPath());
            json.put("isDirectory", file.isDirectory());
            json.put("size", file.isDirectory() ? 0 : (double) file.length());
            json.put("lastModified", (double) file.lastModified());
            json.put("hidden", file.getName().startsWith("."));
            array.put(json);

            if (recursive && file.isDirectory())
                listInto(root, file, true, includeHidden, array);
        }
    }

    private static long sizeOf(File file) {
        if (!file.exists())
            return 0;

        if (file.isFile())
            return file.length();

        long total = 0;
        File[] files = file.listFiles();
        if (files != null) {
            for (File child : files)
                total += sizeOf(child);
        }
        return total;
    }

    private static int countFiles(File file) {
        if (!file.exists())
            return 0;

        if (file.isFile())
            return 1;

        int total = 0;
        File[] files = file.listFiles();
        if (files != null) {
            for (File child : files)
                total += countFiles(child);
        }
        return total;
    }

    public static double getSize(String path) {
        return (double) sizeOf(new File(path));
    }

    private static String collectionPath(String collection) {
        switch (collection == null ? "" : collection.toLowerCase(Locale.ROOT)) {
            case "documents":
                return Environment.DIRECTORY_DOCUMENTS;
            case "pictures":
                return Environment.DIRECTORY_PICTURES;
            case "music":
                return Environment.DIRECTORY_MUSIC;
            case "movies":
                return Environment.DIRECTORY_MOVIES;
            case "dcim":
                return Environment.DIRECTORY_DCIM;
            default:
                return Environment.DIRECTORY_DOWNLOADS;
        }
    }

    private static Uri collectionUri(String collection) {
        if (Build.VERSION.SDK_INT < 29)
            return null;

        switch (collection == null ? "" : collection.toLowerCase(Locale.ROOT)) {
            case "pictures":
            case "dcim":
                return MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            case "music":
                return MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            case "movies":
                return MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            case "documents":
                return MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            default:
                return MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        }
    }

    @SuppressWarnings("deprecation")
    public static String saveToPublic(String source, String collection, String subfolder, String fileName, String mimeType) {
        JSONObject result = new JSONObject();
        Context ctx = context();
        try {
            File input = new File(source);
            if (ctx == null || !input.isFile()) {
                result.put("error", "Source file not found");
                return result.toString();
            }

            String name = fileName == null || fileName.isEmpty() ? input.getName() : fileName;
            String mime = mimeType == null || mimeType.isEmpty() ? ShareProvider.mimeFor(name) : mimeType;
            String relative = collectionPath(collection) + (subfolder == null || subfolder.isEmpty() ? "" : "/" + subfolder);

            if (Build.VERSION.SDK_INT >= 29) {
                ContentResolver resolver = ctx.getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, relative);
                values.put(MediaStore.MediaColumns.IS_PENDING, 1);

                Uri uri = resolver.insert(collectionUri(collection), values);
                if (uri == null) {
                    result.put("error", "MediaStore insert failed");
                    return result.toString();
                }

                try (InputStream in = new FileInputStream(input); OutputStream out = resolver.openOutputStream(uri)) {
                    copyStream(in, out);
                }

                values.clear();
                values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                resolver.update(uri, values, null, null);

                result.put("uri", uri.toString());
                result.put("path", Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + relative + "/" + name);
                return result.toString();
            }

            File dir = new File(Environment.getExternalStorageDirectory(), relative);
            if (!dir.exists() && !dir.mkdirs()) {
                result.put("error", "Could not create " + dir.getAbsolutePath());
                return result.toString();
            }

            File output = new File(dir, name);
            try (InputStream in = new FileInputStream(input); OutputStream out = new FileOutputStream(output)) {
                copyStream(in, out);
            }

            MediaScannerConnection.scanFile(ctx, new String[]{ output.getAbsolutePath() }, new String[]{ mime }, null);
            result.put("uri", Uri.fromFile(output).toString());
            result.put("path", output.getAbsolutePath());
        } catch (Exception e) {
            try {
                result.put("error", e.toString());
            } catch (Exception ignored) {
            }
        }
        return result.toString();
    }

    private static long copyStream(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[128 * 1024];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            total += read;
        }
        out.flush();
        return total;
    }

    private interface Progress {
        void step(String current) throws Exception;
    }

    private static void copyRecursive(File source, File target, boolean overwrite, Progress progress) throws Exception {
        if (source.isDirectory()) {
            if (!target.exists() && !target.mkdirs())
                throw new Exception("Could not create " + target.getAbsolutePath());

            File[] files = source.listFiles();
            if (files != null) {
                for (File child : files)
                    copyRecursive(child, new File(target, child.getName()), overwrite, progress);
            }
            return;
        }

        if (target.exists() && !overwrite) {
            progress.step(target.getAbsolutePath());
            return;
        }

        File parent = target.getParentFile();
        if (parent != null && !parent.exists())
            parent.mkdirs();

        try (InputStream in = new FileInputStream(source); OutputStream out = new FileOutputStream(target)) {
            copyStream(in, out);
        }
        target.setLastModified(source.lastModified());
        progress.step(target.getAbsolutePath());
    }

    private static void deleteRecursive(File file, Progress progress) throws Exception {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files)
                    deleteRecursive(child, progress);
            }
        } else {
            progress.step(file.getAbsolutePath());
        }

        if (file.exists() && !file.delete())
            throw new Exception("Could not delete " + file.getAbsolutePath());
    }

    private static void zipRecursive(File root, File file, ZipOutputStream zip, Progress progress) throws Exception {
        String name = root.toURI().relativize(file.toURI()).getPath();
        if (file.isDirectory()) {
            if (!name.isEmpty()) {
                zip.putNextEntry(new ZipEntry(name.endsWith("/") ? name : name + "/"));
                zip.closeEntry();
            }

            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files)
                    zipRecursive(root, child, zip, progress);
            }
            return;
        }

        ZipEntry entry = new ZipEntry(name.isEmpty() ? file.getName() : name);
        entry.setTime(file.lastModified());
        zip.putNextEntry(entry);
        try (InputStream in = new FileInputStream(file)) {
            copyStream(in, zip);
        }
        zip.closeEntry();
        progress.step(name);
    }

    private static int countZipEntries(File zipFile) {
        int count = 0;
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory())
                    count++;
            }
        } catch (Exception e) {
            return -1;
        }
        return count;
    }

    private static void unzip(File zipFile, File destination, boolean overwrite, Progress progress) throws Exception {
        String root = destination.getCanonicalPath();
        if (!destination.exists())
            destination.mkdirs();

        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                File target = new File(destination, entry.getName());
                String canonical = target.getCanonicalPath();
                if (!canonical.equals(root) && !canonical.startsWith(root + File.separator))
                    throw new Exception("Blocked unsafe zip entry " + entry.getName());

                if (entry.isDirectory()) {
                    target.mkdirs();
                    continue;
                }

                File parent = target.getParentFile();
                if (parent != null && !parent.exists())
                    parent.mkdirs();

                if (!target.exists() || overwrite) {
                    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
                        copyStream(zip, out);
                    }
                    if (entry.getTime() > 0)
                        target.setLastModified(entry.getTime());
                }

                zip.closeEntry();
                progress.step(entry.getName());
            }
        }
    }

    private static void emit(HaxeObject listener, String method, Object... args) {
        if (listener == null)
            return;

        try {
            listener.call(method, args);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    public static void runAsync(final String operation, final String source, final String destination, final boolean overwrite, final HaxeObject listener) {
        new Thread(() -> {
            final int[] done = { 0 };
            String error = "";
            String resultValue = "";

            try {
                File src = new File(source == null ? "" : source);
                File dst = new File(destination == null ? "" : destination);
                final int total;

                switch (operation) {
                    case "unzip":
                        total = countZipEntries(src);
                        break;
                    case "size":
                        total = 0;
                        break;
                    default:
                        total = countFiles(src);
                        break;
                }

                Progress progress = new Progress() {
                    private long lastReport = 0;

                    @Override
                    public void step(String current) {
                        done[0]++;
                        long now = System.currentTimeMillis();
                        if (now - lastReport >= 50 || done[0] == total) {
                            lastReport = now;
                            emit(listener, "onProgress", done[0], total, current);
                        }
                    }
                };

                if (!src.exists())
                    throw new Exception("Source not found: " + src.getAbsolutePath());

                switch (operation) {
                    case "copy":
                        copyRecursive(src, dst, overwrite, progress);
                        resultValue = dst.getAbsolutePath();
                        break;
                    case "move":
                        if (dst.exists() && !overwrite)
                            throw new Exception("Destination already exists: " + dst.getAbsolutePath());

                        if (!dst.exists() && dst.getParentFile() != null)
                            dst.getParentFile().mkdirs();

                        if ((overwrite || !dst.exists()) && src.renameTo(dst)) {
                            done[0] = total;
                            emit(listener, "onProgress", total, total, dst.getAbsolutePath());
                        } else {
                            copyRecursive(src, dst, overwrite, progress);
                            deleteRecursive(src, new Progress() {
                                @Override
                                public void step(String current) {
                                }
                            });
                        }
                        resultValue = dst.getAbsolutePath();
                        break;
                    case "delete":
                        deleteRecursive(src, progress);
                        break;
                    case "zip":
                        File parent = dst.getParentFile();
                        if (parent != null && !parent.exists())
                            parent.mkdirs();

                        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(dst)))) {
                            File base = src.isDirectory() ? src : src.getParentFile();
                            zipRecursive(base, src, zip, progress);
                        }
                        resultValue = dst.getAbsolutePath();
                        break;
                    case "unzip":
                        unzip(src, dst, overwrite, progress);
                        resultValue = dst.getAbsolutePath();
                        break;
                    case "size":
                        resultValue = String.valueOf(sizeOf(src));
                        break;
                    default:
                        throw new Exception("Unknown operation " + operation);
                }
            } catch (Throwable e) {
                error = e.getMessage() != null ? e.getMessage() : e.toString();
                Log.e(Utils.LOG_TAG, error);
            }

            emit(listener, "onComplete", error.isEmpty(), error.isEmpty() ? resultValue : error, done[0]);
        }, "AndroidManager-Storage").start();
    }
}
