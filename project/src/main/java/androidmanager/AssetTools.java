package androidmanager;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AssetTools {

    private static final int BUFFER_SIZE = 262144;

    private static final AtomicInteger batchCompleted = new AtomicInteger(0);
    private static final AtomicInteger batchTotal = new AtomicInteger(0);
    private static final AtomicLong batchBytes = new AtomicLong(0);
    private static final AtomicBoolean batchCancelled = new AtomicBoolean(false);
    private static volatile boolean batchRunning = false;
    private static volatile long batchStarted = 0;
    private static volatile long batchFinished = 0;
    private static final List<String> batchFailed = Collections.synchronizedList(new ArrayList<String>());
    private static final Set<String> createdDirectories = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private static AssetManager assets() {
        Context context = Utils.context();
        return context != null ? context.getAssets() : null;
    }

    public static boolean exists(String asset) {
        AssetManager manager = assets();
        if (manager == null || asset == null)
            return false;

        InputStream input = null;
        try {
            input = manager.open(asset);
            return true;
        } catch (Exception e) {
            try {
                String[] children = manager.list(asset);
                return children != null && children.length > 0;
            } catch (Exception ignored) {
                return false;
            }
        } finally {
            close(input);
        }
    }

    public static String list(String path) {
        AssetManager manager = assets();
        if (manager == null)
            return "";

        try {
            String[] children = manager.list(path == null ? "" : path);
            if (children == null)
                return "";

            StringBuilder builder = new StringBuilder();
            for (String child : children) {
                if (builder.length() > 0)
                    builder.append('\n');
                builder.append(child);
            }
            return builder.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String readText(String asset) {
        AssetManager manager = assets();
        if (manager == null || asset == null)
            return null;

        InputStream input = null;
        try {
            input = manager.open(asset);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[16384];
            int read;
            while ((read = input.read(buffer)) != -1)
                output.write(buffer, 0, read);
            return output.toString("UTF-8");
        } catch (Exception e) {
            return null;
        } finally {
            close(input);
        }
    }

    public static double size(String asset) {
        AssetManager manager = assets();
        if (manager == null || asset == null)
            return -1;

        try {
            return manager.openFd(asset).getLength();
        } catch (Exception e) {
            InputStream input = null;
            try {
                input = manager.open(asset);
                long total = 0;
                byte[] buffer = new byte[65536];
                int read;
                while ((read = input.read(buffer)) != -1)
                    total += read;
                return total;
            } catch (Exception ignored) {
                return -1;
            } finally {
                close(input);
            }
        }
    }

    public static boolean copy(String asset, String target) {
        AssetManager manager = assets();
        if (manager == null || asset == null || target == null)
            return false;

        File file = new File(target);
        if (!ensureParent(file, false))
            return false;

        try {
            return stream(manager.open(asset), file) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static int copyFolder(String folder, String target, boolean overwrite) {
        AssetManager manager = assets();
        if (manager == null || folder == null || target == null)
            return 0;

        int copied = 0;
        try {
            String[] children = manager.list(folder);
            if (children == null || children.length == 0) {
                File file = new File(target);
                if (!overwrite && file.exists())
                    return 0;
                return copy(folder, target) ? 1 : 0;
            }

            for (String child : children) {
                String source = folder.isEmpty() ? child : folder + "/" + child;
                copied += copyFolder(source, target + "/" + child, overwrite);
            }
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
        return copied;
    }

    public static boolean startBatch(String jobs, int threads) {
        if (batchRunning || jobs == null)
            return false;

        final String[] lines = jobs.isEmpty() ? new String[0] : jobs.split("\n");
        batchCompleted.set(0);
        batchTotal.set(lines.length);
        batchBytes.set(0);
        batchCancelled.set(false);
        batchFailed.clear();
        createdDirectories.clear();
        batchStarted = System.currentTimeMillis();
        batchFinished = 0;
        batchRunning = true;

        final int poolSize = Math.max(1, Math.min(threads <= 0 ? 4 : threads, Runtime.getRuntime().availableProcessors()));
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                ExecutorService pool = Executors.newFixedThreadPool(poolSize);
                for (final String line : lines) {
                    pool.execute(new Runnable() {
                        @Override
                        public void run() {
                            boolean ok = false;
                            if (!batchCancelled.get()) {
                                try {
                                    String[] parts = line.split("\t");
                                    if (parts.length == 3)
                                        ok = copyJob(parts[0], parts[1], parts[2]);
                                } catch (Exception ignored) {
                                }
                            }
                            if (!ok)
                                batchFailed.add(line);
                            batchCompleted.incrementAndGet();
                        }
                    });
                }
                pool.shutdown();
                try {
                    pool.awaitTermination(1, TimeUnit.HOURS);
                } catch (InterruptedException ignored) {
                }
                batchFinished = System.currentTimeMillis();
                batchRunning = false;
            }
        }, "android-manager-assets");
        worker.setPriority(Thread.NORM_PRIORITY);
        worker.start();
        return true;
    }

    public static void cancelBatch() {
        batchCancelled.set(true);
    }

    public static int getBatchCompleted() {
        return batchCompleted.get();
    }

    public static int getBatchTotal() {
        return batchTotal.get();
    }

    public static double getBatchBytes() {
        return batchBytes.get();
    }

    public static double getBatchElapsed() {
        if (batchStarted == 0)
            return 0;
        long end = batchRunning || batchFinished == 0 ? System.currentTimeMillis() : batchFinished;
        return (end - batchStarted) / 1000.0;
    }

    public static boolean isBatchRunning() {
        return batchRunning;
    }

    public static String getBatchFailed() {
        synchronized (batchFailed) {
            StringBuilder builder = new StringBuilder();
            for (String line : batchFailed) {
                if (builder.length() > 0)
                    builder.append('\n');
                builder.append(line);
            }
            return builder.toString();
        }
    }

    private static boolean copyJob(String type, String source, String target) {
        File file = new File(target);
        if (!ensureParent(file, true))
            return false;

        try {
            InputStream input;
            if ("A".equals(type)) {
                AssetManager manager = assets();
                if (manager == null)
                    return false;
                input = manager.open(source);
            } else {
                input = new FileInputStream(source);
            }
            long written = stream(input, file);
            if (written < 0)
                return false;
            batchBytes.addAndGet(written);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean ensureParent(File target, boolean cached) {
        File parent = target.getParentFile();
        if (parent == null)
            return true;

        String key = parent.getAbsolutePath();
        if (cached && createdDirectories.contains(key))
            return true;

        if (parent.exists() || parent.mkdirs() || parent.exists()) {
            if (cached)
                createdDirectories.add(key);
            return true;
        }
        return false;
    }

    private static long stream(InputStream input, File target) {
        File temp = new File(target.getAbsolutePath() + ".tmp");
        OutputStream output = null;
        long total = 0;
        try {
            output = new BufferedOutputStream(new FileOutputStream(temp), BUFFER_SIZE);
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (batchCancelled.get() && batchRunning)
                    throw new InterruptedException("cancelled");
                output.write(buffer, 0, read);
                total += read;
            }
            output.close();
            output = null;
            if (target.exists() && !target.delete()) {
                temp.delete();
                return -1;
            }
            return temp.renameTo(target) ? total : -1;
        } catch (Exception e) {
            temp.delete();
            return -1;
        } finally {
            close(input);
            close(output);
        }
    }

    private static void close(java.io.Closeable closeable) {
        if (closeable == null)
            return;
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
