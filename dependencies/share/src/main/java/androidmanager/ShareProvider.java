package androidmanager;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class ShareProvider extends ContentProvider {

    public static String authority(Context context) {
        return context.getPackageName() + ".androidmanager.share";
    }

    @SuppressWarnings("deprecation")
    private static Map<String, File> roots(Context context) {
        Map<String, File> roots = new LinkedHashMap<>();
        put(roots, "files", context.getFilesDir());
        put(roots, "cache", context.getCacheDir());
        put(roots, "external", context.getExternalFilesDir(null));
        put(roots, "external_cache", context.getExternalCacheDir());

        File[] media = context.getExternalMediaDirs();
        if (media != null && media.length > 0)
            put(roots, "media", media[0]);

        put(roots, "obb", context.getObbDir());
        put(roots, "storage", Environment.getExternalStorageDirectory());
        return roots;
    }

    private static void put(Map<String, File> roots, String name, File dir) {
        if (dir == null)
            return;

        try {
            roots.put(name, dir.getCanonicalFile());
        } catch (Exception e) {
            roots.put(name, dir.getAbsoluteFile());
        }
    }

    public static Uri getUriForFile(Context context, String path) {
        if (context == null || path == null || path.isEmpty())
            return null;

        try {
            String target = new File(path).getCanonicalPath();
            String bestName = null;
            String bestPath = null;

            for (Map.Entry<String, File> entry : roots(context).entrySet()) {
                String root = entry.getValue().getPath();
                if ((target.equals(root) || target.startsWith(root + File.separator)) && (bestPath == null || root.length() > bestPath.length())) {
                    bestName = entry.getKey();
                    bestPath = root;
                }
            }

            if (bestName == null)
                return null;

            String relative = target.substring(bestPath.length());
            if (relative.startsWith(File.separator))
                relative = relative.substring(1);

            return new Uri.Builder()
                .scheme("content")
                .authority(authority(context))
                .encodedPath(Uri.encode(bestName) + "/" + Uri.encode(relative, "/"))
                .build();
        } catch (Exception e) {
            return null;
        }
    }

    private File fileForUri(Uri uri) throws FileNotFoundException {
        Context context = getContext();
        if (context == null || uri == null || uri.getPath() == null)
            throw new FileNotFoundException("Invalid uri");

        String path = uri.getEncodedPath();
        if (path.startsWith("/"))
            path = path.substring(1);

        int split = path.indexOf('/');
        if (split <= 0)
            throw new FileNotFoundException("Invalid uri");

        String rootName = Uri.decode(path.substring(0, split));
        String relative = Uri.decode(path.substring(split + 1));
        File root = roots(context).get(rootName);
        if (root == null)
            throw new FileNotFoundException("Unknown root " + rootName);

        try {
            File file = new File(root, relative).getCanonicalFile();
            if (!file.getPath().startsWith(root.getPath() + File.separator) && !file.equals(root))
                throw new SecurityException("Path outside of shared root");
            return file;
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new FileNotFoundException(e.toString());
        }
    }

    public static String mimeFor(String name) {
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            String extension = name.substring(dot + 1).toLowerCase(Locale.ROOT);
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null)
                return mime;

            switch (extension) {
                case "json":
                    return "application/json";
                case "hx":
                case "hxs":
                case "hscript":
                case "lua":
                case "xml":
                case "txt":
                    return "text/plain";
                case "webp":
                    return "image/webp";
                case "ogg":
                    return "audio/ogg";
                case "zip":
                case "fnfc":
                    return "application/zip";
                default:
                    break;
            }
        }
        return "application/octet-stream";
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        File file;
        try {
            file = fileForUri(uri);
        } catch (Exception e) {
            return null;
        }

        String[] columns = projection != null ? projection : new String[]{ OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE };
        String[] outColumns = new String[columns.length];
        Object[] values = new Object[columns.length];
        int count = 0;

        for (String column : columns) {
            if (OpenableColumns.DISPLAY_NAME.equals(column)) {
                outColumns[count] = OpenableColumns.DISPLAY_NAME;
                values[count++] = file.getName();
            } else if (OpenableColumns.SIZE.equals(column)) {
                outColumns[count] = OpenableColumns.SIZE;
                values[count++] = file.length();
            }
        }

        String[] finalColumns = new String[count];
        Object[] finalValues = new Object[count];
        System.arraycopy(outColumns, 0, finalColumns, 0, count);
        System.arraycopy(values, 0, finalValues, 0, count);

        MatrixCursor cursor = new MatrixCursor(finalColumns, 1);
        cursor.addRow(finalValues);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        try {
            return mimeFor(fileForUri(uri).getName());
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = fileForUri(uri);
        if (!"r".equals(mode))
            throw new SecurityException("Shared files are read only");

        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Read only provider");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read only provider");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read only provider");
    }
}
