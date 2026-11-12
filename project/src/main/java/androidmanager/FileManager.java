package java.androidmanager;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.haxe.lime.HaxeObject;
import org.json.JSONArray;
import org.json.JSONObject;

public class FileManager {

    private static ContentResolver resolver() {
        Context ctx = Utils.context();
        return ctx != null ? ctx.getContentResolver() : null;
    }

    private static String[] splitMimeTypes(String mimeTypes) {
        if (mimeTypes == null || mimeTypes.trim().isEmpty())
            return new String[]{ "*/*" };

        String[] parts = mimeTypes.split(",");
        for (int i = 0; i < parts.length; i++)
            parts[i] = parts[i].trim();
        return parts;
    }

    public static boolean pickFile(int requestCode, String mimeTypes, boolean multiple) {
        String[] types = splitMimeTypes(mimeTypes);

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(types.length == 1 ? types[0] : "*/*");
        if (types.length > 1)
            intent.putExtra(Intent.EXTRA_MIME_TYPES, types);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        return Utils.startForResult(intent, requestCode);
    }

    public static boolean pickDirectory(int requestCode, String initialUri) {
        if (Build.VERSION.SDK_INT < 21)
            return false;

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);

        if (Build.VERSION.SDK_INT >= 26 && initialUri != null && !initialUri.isEmpty())
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(initialUri));

        return Utils.startForResult(intent, requestCode);
    }

    public static boolean createDocument(int requestCode, String fileName, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType == null || mimeType.isEmpty() ? "application/octet-stream" : mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        return Utils.startForResult(intent, requestCode);
    }

    public static boolean takePersistablePermission(String uri) {
        ContentResolver cr = resolver();
        if (cr == null || uri == null)
            return false;

        try {
            cr.takePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            return true;
        } catch (Exception e) {
            try {
                cr.takePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    public static void releasePersistablePermission(String uri) {
        ContentResolver cr = resolver();
        if (cr == null || uri == null)
            return;

        try {
            cr.releasePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    public static String getPersistedUris() {
        JSONArray array = new JSONArray();
        ContentResolver cr = resolver();
        if (cr == null)
            return array.toString();

        for (UriPermission permission : cr.getPersistedUriPermissions())
            array.put(permission.getUri().toString());

        return array.toString();
    }

    public static String getUriInfo(String uri) {
        JSONObject json = new JSONObject();
        ContentResolver cr = resolver();
        if (cr == null || uri == null)
            return json.toString();

        Cursor cursor = null;
        try {
            Uri parsed = Uri.parse(uri);
            json.put("uri", uri);
            json.put("mimeType", cr.getType(parsed) != null ? cr.getType(parsed) : "");

            cursor = cr.query(parsed, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                int modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED);

                json.put("name", nameIndex >= 0 ? cursor.getString(nameIndex) : parsed.getLastPathSegment());
                json.put("size", sizeIndex >= 0 && !cursor.isNull(sizeIndex) ? (double) cursor.getLong(sizeIndex) : -1);
                json.put("lastModified", modifiedIndex >= 0 && !cursor.isNull(modifiedIndex) ? (double) cursor.getLong(modifiedIndex) : -1);
            }
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return json.toString();
    }

    private static long copyStream(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[64 * 1024];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            total += read;
        }
        out.flush();
        return total;
    }

    public static boolean copyUriToFile(String uri, String path) {
        ContentResolver cr = resolver();
        if (cr == null || uri == null || path == null)
            return false;

        try {
            File target = new File(path);
            File parent = target.getParentFile();
            if (parent != null && !parent.exists())
                parent.mkdirs();

            try (InputStream in = cr.openInputStream(Uri.parse(uri)); OutputStream out = new FileOutputStream(target)) {
                if (in == null)
                    return false;
                copyStream(in, out);
            }
            return true;
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return false;
        }
    }

    public static boolean copyFileToUri(String path, String uri) {
        ContentResolver cr = resolver();
        if (cr == null || uri == null || path == null)
            return false;

        try (InputStream in = new FileInputStream(path); OutputStream out = cr.openOutputStream(Uri.parse(uri), "wt")) {
            if (out == null)
                return false;
            copyStream(in, out);
            return true;
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return false;
        }
    }

    public static String readUriAsString(String uri) {
        ContentResolver cr = resolver();
        if (cr == null || uri == null)
            return null;

        try (InputStream in = cr.openInputStream(Uri.parse(uri))) {
            if (in == null)
                return null;

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            copyStream(in, out);
            return out.toString("UTF-8");
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return null;
        }
    }

    public static boolean writeStringToUri(String uri, String content) {
        ContentResolver cr = resolver();
        if (cr == null || uri == null)
            return false;

        try (OutputStream out = cr.openOutputStream(Uri.parse(uri), "wt")) {
            if (out == null)
                return false;
            out.write((content == null ? "" : content).getBytes("UTF-8"));
            out.flush();
            return true;
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return false;
        }
    }

    private static Uri documentUriFromTree(Uri treeUri) {
        String documentId = DocumentsContract.isDocumentUri(Utils.context(), treeUri)
            ? DocumentsContract.getDocumentId(treeUri)
            : DocumentsContract.getTreeDocumentId(treeUri);
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
    }

    private static JSONArray listChildren(Uri treeUri, Uri documentUri) throws Exception {
        JSONArray array = new JSONArray();
        ContentResolver cr = resolver();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, DocumentsContract.getDocumentId(documentUri));

        String[] projection = {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        };

        try (Cursor cursor = cr.query(childrenUri, projection, null, null, null)) {
            if (cursor == null)
                return array;

            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                String mime = cursor.getString(2);
                JSONObject entry = new JSONObject();
                entry.put("name", cursor.getString(1));
                entry.put("uri", DocumentsContract.buildDocumentUriUsingTree(treeUri, id).toString());
                entry.put("mimeType", mime != null ? mime : "");
                entry.put("isDirectory", DocumentsContract.Document.MIME_TYPE_DIR.equals(mime));
                entry.put("size", cursor.isNull(3) ? -1 : (double) cursor.getLong(3));
                entry.put("lastModified", cursor.isNull(4) ? -1 : (double) cursor.getLong(4));
                array.put(entry);
            }
        }
        return array;
    }

    public static String listDirectory(String treeUri) {
        if (treeUri == null || Build.VERSION.SDK_INT < 21)
            return "[]";

        try {
            Uri tree = Uri.parse(treeUri);
            return listChildren(tree, documentUriFromTree(tree)).toString();
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return "[]";
        }
    }

    private static int copyTree(Uri treeUri, Uri documentUri, File destination, HaxeObject progress, int[] counter) throws Exception {
        if (!destination.exists())
            destination.mkdirs();

        JSONArray children = listChildren(treeUri, documentUri);
        for (int i = 0; i < children.length(); i++) {
            JSONObject child = children.getJSONObject(i);
            String name = child.getString("name");
            Uri childUri = Uri.parse(child.getString("uri"));
            File target = new File(destination, name);

            if (child.getBoolean("isDirectory")) {
                copyTree(treeUri, childUri, target, progress, counter);
                continue;
            }

            try (InputStream in = resolver().openInputStream(childUri); OutputStream out = new FileOutputStream(target)) {
                if (in != null)
                    copyStream(in, out);
            }

            counter[0]++;
            if (progress != null) {
                try {
                    progress.call("onProgress", new Object[]{ counter[0], target.getAbsolutePath() });
                } catch (Exception ignored) {
                }
            }
        }
        return counter[0];
    }

    public static int copyTreeToPath(String treeUri, String path) {
        if (treeUri == null || path == null || Build.VERSION.SDK_INT < 21)
            return -1;

        try {
            Uri tree = Uri.parse(treeUri);
            return copyTree(tree, documentUriFromTree(tree), new File(path), null, new int[]{ 0 });
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return -1;
        }
    }

    public static void copyTreeToPathAsync(final String treeUri, final String path, final HaxeObject listener) {
        new Thread(() -> {
            int count = -1;
            String error = "";
            try {
                Uri tree = Uri.parse(treeUri);
                count = copyTree(tree, documentUriFromTree(tree), new File(path), listener, new int[]{ 0 });
            } catch (Exception e) {
                error = e.toString();
                Log.e(Utils.LOG_TAG, error);
            }

            if (listener != null) {
                try {
                    listener.call("onComplete", new Object[]{ count, error });
                } catch (Exception ignored) {
                }
            }
        }, "AndroidManager-CopyTree").start();
    }

    public static boolean deleteDocument(String uri) {
        ContentResolver cr = resolver();
        if (cr == null || uri == null)
            return false;

        try {
            return DocumentsContract.deleteDocument(cr, Uri.parse(uri));
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return false;
        }
    }

    public static boolean openUri(String uri, String mimeType) {
        if (uri == null)
            return false;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri parsed = Uri.parse(uri);
        if (mimeType != null && !mimeType.isEmpty())
            intent.setDataAndType(parsed, mimeType);
        else
            intent.setData(parsed);

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        return Utils.startActivity(intent);
    }

    public static void scanMedia(String path) {
        Context ctx = Utils.context();
        if (ctx == null || path == null)
            return;

        MediaScannerConnection.scanFile(ctx, new String[]{ path }, null, null);
    }
}
