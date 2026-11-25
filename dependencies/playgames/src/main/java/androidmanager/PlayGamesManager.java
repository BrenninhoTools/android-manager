package androidmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.AuthenticationResult;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayerLevelInfo;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.achievement.Achievement;
import com.google.android.gms.games.achievement.AchievementBuffer;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.ScoreSubmissionData;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataBuffer;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import java.io.File;
import java.io.FileOutputStream;
import org.haxe.extension.Extension;
import org.haxe.lime.HaxeObject;
import org.json.JSONArray;
import org.json.JSONObject;

public class PlayGamesManager extends Extension {

    private static final String TAG = "AndroidManager";

    public static final int MODE_AUTO = 0;
    public static final int MODE_MANUAL = 1;
    public static final int MODE_SILENT = 2;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_FAILED = 3;

    private static final String PREFS = "android_manager_play_games";

    private static boolean initialized = false;
    private static boolean authenticated = false;
    private static HaxeObject listener = null;

    private static int mode = MODE_AUTO;
    private static int state = STATE_DISCONNECTED;
    private static String lastError = "";
    private static int maxAutoPrompts = 1;
    private static int autoPrompts = 0;
    private static boolean busy = false;
    private static boolean firstResume = true;
    private static boolean rememberDecline = true;
    private static boolean checkOnResume = true;

    private static Activity activity() {
        return Extension.mainActivity;
    }

    private static void respond(final HaxeObject callback, final boolean success, final String data) {
        if (callback == null)
            return;

        try {
            callback.call("onResult", new Object[]{ success, data == null ? "" : data });
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private static String error(Exception e) {
        try {
            JSONObject json = new JSONObject();
            if (e instanceof ApiException)
                json.put("code", ((ApiException) e).getStatusCode());
            else
                json.put("code", -1);
            json.put("message", e != null ? String.valueOf(e.getMessage()) : "Unknown error");
            return json.toString();
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private static boolean ready(HaxeObject callback) {
        if (activity() == null) {
            respond(callback, false, "{\"code\":-1,\"message\":\"Activity not available\"}");
            return false;
        }

        init();
        return true;
    }

    private static <T> void onTask(Task<T> task, final HaxeObject callback, final Converter<T> converter) {
        task.addOnCompleteListener(new OnCompleteListener<T>() {
            @Override
            public void onComplete(Task<T> result) {
                if (!result.isSuccessful()) {
                    respond(callback, false, error(result.getException()));
                    return;
                }

                try {
                    respond(callback, true, converter != null ? converter.convert(result.getResult()) : "");
                } catch (Exception e) {
                    respond(callback, false, error(e));
                }
            }
        });
    }

    private interface Converter<T> {
        String convert(T value) throws Exception;
    }

    private static void startIntent(Task<Intent> task, final int requestCode, final HaxeObject callback) {
        task.addOnCompleteListener(new OnCompleteListener<Intent>() {
            @Override
            public void onComplete(Task<Intent> result) {
                if (!result.isSuccessful() || activity() == null) {
                    respond(callback, false, error(result.getException()));
                    return;
                }

                try {
                    activity().startActivityForResult(result.getResult(), requestCode);
                    respond(callback, true, "");
                } catch (Exception e) {
                    respond(callback, false, error(e));
                }
            }
        });
    }

    private static void setAuthenticated(boolean value) {
        boolean changed = authenticated != value;
        authenticated = value;

        HaxeObject target = listener;
        if (changed && target != null) {
            try {
                target.call("onAuthChanged", new Object[]{ value });
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        init();
        loadConfig();
    }

    @Override
    public void onResume() {
        if (busy || activity() == null)
            return;

        if (firstResume) {
            firstResume = false;
            autoConnect();
        } else if (!authenticated && checkOnResume) {
            silentCheck(null);
        }
    }

    private static int resourceInt(String name, int fallback) {
        try {
            String value = resourceString(name);
            return value == null || value.isEmpty() ? fallback : Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String resourceString(String name) {
        Context ctx = Extension.mainContext != null ? Extension.mainContext : activity();
        if (ctx == null)
            return null;

        int id = ctx.getResources().getIdentifier(name, "string", ctx.getPackageName());
        return id != 0 ? ctx.getString(id) : null;
    }

    private static void loadConfig() {
        String value = resourceString("android_manager_play_games_sign_in");
        if (value != null) {
            value = value.trim().toLowerCase();
            if (value.equals("manual"))
                mode = MODE_MANUAL;
            else if (value.equals("silent"))
                mode = MODE_SILENT;
            else
                mode = MODE_AUTO;
        }

        maxAutoPrompts = resourceInt("android_manager_play_games_auto_prompts", 1);
    }

    private static SharedPreferences prefs() {
        Context ctx = Extension.mainContext != null ? Extension.mainContext : activity();
        return ctx != null ? ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE) : null;
    }

    public static boolean isDeclined() {
        SharedPreferences p = prefs();
        return p != null && p.getBoolean("declined", false);
    }

    private static void setDeclined(boolean value) {
        SharedPreferences p = prefs();
        if (p != null)
            p.edit().putBoolean("declined", value).apply();
    }

    public static void resetDeclined() {
        setDeclined(false);
        autoPrompts = 0;
    }

    public static void setMode(int value) {
        mode = value;
    }

    public static int getMode() {
        return mode;
    }

    public static int getState() {
        return state;
    }

    public static String getLastError() {
        return lastError;
    }

    public static void setMaxAutoPrompts(int value) {
        maxAutoPrompts = value;
    }

    public static void setRememberDecline(boolean value) {
        rememberDecline = value;
    }

    public static void setCheckOnResume(boolean value) {
        checkOnResume = value;
    }

    private static void setState(int value, String error) {
        lastError = error == null ? "" : error;
        if (state == value)
            return;

        state = value;
        HaxeObject target = listener;
        if (target != null) {
            try {
                target.call("onStateChanged", new Object[]{ value, lastError });
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    private static void silentCheck(final Runnable onFailed) {
        if (activity() == null)
            return;

        init();
        busy = true;
        if (!authenticated)
            setState(STATE_CONNECTING, null);

        PlayGames.getGamesSignInClient(activity()).isAuthenticated().addOnCompleteListener(new OnCompleteListener<AuthenticationResult>() {
            @Override
            public void onComplete(Task<AuthenticationResult> task) {
                boolean ok = task.isSuccessful() && task.getResult() != null && task.getResult().isAuthenticated();
                setAuthenticated(ok);
                busy = false;

                if (ok) {
                    setState(STATE_CONNECTED, null);
                } else if (onFailed != null) {
                    onFailed.run();
                } else {
                    setState(STATE_DISCONNECTED, task.isSuccessful() ? null : error(task.getException()));
                }
            }
        });
    }

    private static void prompt(final HaxeObject callback, final boolean automatic) {
        if (activity() == null) {
            respond(callback, false, "{\"code\":-1,\"message\":\"Activity not available\"}");
            return;
        }

        init();
        busy = true;
        setState(STATE_CONNECTING, null);

        PlayGames.getGamesSignInClient(activity()).signIn().addOnCompleteListener(new OnCompleteListener<AuthenticationResult>() {
            @Override
            public void onComplete(Task<AuthenticationResult> task) {
                boolean ok = task.isSuccessful() && task.getResult() != null && task.getResult().isAuthenticated();
                setAuthenticated(ok);
                busy = false;

                if (ok) {
                    setDeclined(false);
                    setState(STATE_CONNECTED, null);
                    respond(callback, true, "true");
                    return;
                }

                String message = task.isSuccessful() ? "{\"code\":-1,\"message\":\"Sign in canceled\"}" : error(task.getException());
                if (automatic && rememberDecline)
                    setDeclined(true);

                setState(STATE_FAILED, message);
                respond(callback, false, message);
            }
        });
    }

    public static void autoConnect() {
        if (busy || authenticated)
            return;

        silentCheck(new Runnable() {
            @Override
            public void run() {
                boolean canPrompt = mode == MODE_AUTO
                    && autoPrompts < maxAutoPrompts
                    && !(rememberDecline && isDeclined());

                if (canPrompt) {
                    autoPrompts++;
                    prompt(null, true);
                } else {
                    setState(STATE_DISCONNECTED, null);
                }
            }
        });
    }

    public static void connect(HaxeObject callback) {
        if (authenticated) {
            respond(callback, true, "true");
            return;
        }

        if (busy) {
            respond(callback, false, "{\"code\":-2,\"message\":\"Sign in already in progress\"}");
            return;
        }

        setDeclined(false);
        prompt(callback, false);
    }

    public static void init() {
        if (initialized)
            return;

        Context ctx = Extension.mainContext != null ? Extension.mainContext : activity();
        if (ctx == null)
            return;

        try {
            PlayGamesSdk.initialize(ctx);
            initialized = true;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void setListener(HaxeObject object) {
        listener = object;
    }

    public static boolean isSignedIn() {
        return authenticated;
    }

    private static Converter<AuthenticationResult> authConverter() {
        return new Converter<AuthenticationResult>() {
            @Override
            public String convert(AuthenticationResult value) {
                boolean ok = value != null && value.isAuthenticated();
                setAuthenticated(ok);
                if (ok)
                    setState(STATE_CONNECTED, null);
                else if (state == STATE_CONNECTED)
                    setState(STATE_DISCONNECTED, null);
                return ok ? "true" : "false";
            }
        };
    }

    public static void checkAuthentication(final HaxeObject callback) {
        if (!ready(callback))
            return;

        GamesSignInClient client = PlayGames.getGamesSignInClient(activity());
        onTask(client.isAuthenticated(), callback, authConverter());
    }

    public static void signIn(final HaxeObject callback) {
        if (!ready(callback))
            return;

        connect(callback);
    }

    public static void requestServerSideAccess(String serverClientId, boolean forceRefreshToken, final HaxeObject callback) {
        if (!ready(callback))
            return;

        onTask(PlayGames.getGamesSignInClient(activity()).requestServerSideAccess(serverClientId, forceRefreshToken), callback, new Converter<String>() {
            @Override
            public String convert(String value) {
                return value;
            }
        });
    }

    private static JSONObject playerToJson(Player player) throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", player.getPlayerId());
        json.put("displayName", player.getDisplayName());
        json.put("title", player.getTitle() != null ? player.getTitle() : "");
        json.put("iconImageUri", player.getIconImageUri() != null ? player.getIconImageUri().toString() : "");
        json.put("hiResImageUri", player.getHiResImageUri() != null ? player.getHiResImageUri().toString() : "");

        PlayerLevelInfo level = player.getLevelInfo();
        if (level != null) {
            json.put("level", level.getCurrentLevel() != null ? level.getCurrentLevel().getLevelNumber() : 0);
            json.put("xp", (double) level.getCurrentXpTotal());
        } else {
            json.put("level", 0);
            json.put("xp", 0);
        }
        return json;
    }

    public static void getCurrentPlayer(final HaxeObject callback) {
        if (!ready(callback))
            return;

        onTask(PlayGames.getPlayersClient(activity()).getCurrentPlayer(), callback, new Converter<Player>() {
            @Override
            public String convert(Player value) throws Exception {
                return playerToJson(value).toString();
            }
        });
    }

    public static void savePlayerImage(final String path, final boolean hiRes, final HaxeObject callback) {
        if (!ready(callback))
            return;

        PlayGames.getPlayersClient(activity()).getCurrentPlayer().addOnCompleteListener(new OnCompleteListener<Player>() {
            @Override
            public void onComplete(Task<Player> task) {
                if (!task.isSuccessful() || task.getResult() == null) {
                    respond(callback, false, error(task.getException()));
                    return;
                }

                Player player = task.getResult();
                final Uri uri = hiRes && player.getHiResImageUri() != null ? player.getHiResImageUri() : player.getIconImageUri();
                if (uri == null) {
                    respond(callback, false, "{\"code\":-1,\"message\":\"Player has no image\"}");
                    return;
                }

                final Activity act = activity();
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageManager.create(act).loadImage(new ImageManager.OnImageLoadedListener() {
                            @Override
                            public void onImageLoaded(Uri loaded, Drawable drawable, boolean isRequested) {
                                if (drawable == null) {
                                    respond(callback, false, "{\"code\":-1,\"message\":\"Image could not be loaded\"}");
                                    return;
                                }

                                try {
                                    Bitmap bitmap;
                                    if (drawable instanceof BitmapDrawable && ((BitmapDrawable) drawable).getBitmap() != null) {
                                        bitmap = ((BitmapDrawable) drawable).getBitmap();
                                    } else {
                                        int w = Math.max(1, drawable.getIntrinsicWidth());
                                        int h = Math.max(1, drawable.getIntrinsicHeight());
                                        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                                        Canvas canvas = new Canvas(bitmap);
                                        drawable.setBounds(0, 0, w, h);
                                        drawable.draw(canvas);
                                    }

                                    File file = new File(path);
                                    File parent = file.getParentFile();
                                    if (parent != null && !parent.exists())
                                        parent.mkdirs();

                                    FileOutputStream out = new FileOutputStream(file);
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                                    out.flush();
                                    out.close();
                                    respond(callback, true, file.getAbsolutePath());
                                } catch (Exception e) {
                                    respond(callback, false, error(e));
                                }
                            }
                        }, uri);
                    }
                });
            }
        });
    }

    public static void unlockAchievement(String id, HaxeObject callback) {
        if (!ready(callback))
            return;

        AchievementsClient client = PlayGames.getAchievementsClient(activity());
        if (callback == null)
            client.unlock(id);
        else
            onTask(client.unlockImmediate(id), callback, null);
    }

    public static void incrementAchievement(String id, int steps, HaxeObject callback) {
        if (!ready(callback))
            return;

        AchievementsClient client = PlayGames.getAchievementsClient(activity());
        if (callback == null)
            client.increment(id, steps);
        else
            onTask(client.incrementImmediate(id, steps), callback, new Converter<Boolean>() {
                @Override
                public String convert(Boolean value) {
                    return value != null && value ? "true" : "false";
                }
            });
    }

    public static void setAchievementSteps(String id, int steps, HaxeObject callback) {
        if (!ready(callback))
            return;

        AchievementsClient client = PlayGames.getAchievementsClient(activity());
        if (callback == null)
            client.setSteps(id, steps);
        else
            onTask(client.setStepsImmediate(id, steps), callback, new Converter<Boolean>() {
                @Override
                public String convert(Boolean value) {
                    return value != null && value ? "true" : "false";
                }
            });
    }

    public static void revealAchievement(String id, HaxeObject callback) {
        if (!ready(callback))
            return;

        AchievementsClient client = PlayGames.getAchievementsClient(activity());
        if (callback == null)
            client.reveal(id);
        else
            onTask(client.revealImmediate(id), callback, null);
    }

    public static void showAchievements(int requestCode, HaxeObject callback) {
        if (!ready(callback))
            return;

        startIntent(PlayGames.getAchievementsClient(activity()).getAchievementsIntent(), requestCode, callback);
    }

    public static void loadAchievements(boolean forceReload, HaxeObject callback) {
        if (!ready(callback))
            return;

        onTask(PlayGames.getAchievementsClient(activity()).load(forceReload), callback, new Converter<AnnotatedData<AchievementBuffer>>() {
            @Override
            public String convert(AnnotatedData<AchievementBuffer> value) throws Exception {
                JSONArray array = new JSONArray();
                AchievementBuffer buffer = value.get();
                if (buffer == null)
                    return array.toString();

                try {
                    for (int i = 0; i < buffer.getCount(); i++) {
                        Achievement achievement = buffer.get(i);
                        JSONObject json = new JSONObject();
                        boolean incremental = achievement.getType() == Achievement.TYPE_INCREMENTAL;
                        json.put("id", achievement.getAchievementId());
                        json.put("name", achievement.getName());
                        json.put("description", achievement.getDescription());
                        json.put("state", achievement.getState());
                        json.put("incremental", incremental);
                        json.put("currentSteps", incremental ? achievement.getCurrentSteps() : 0);
                        json.put("totalSteps", incremental ? achievement.getTotalSteps() : 0);
                        json.put("xp", (double) achievement.getXpValue());
                        json.put("lastUpdated", (double) achievement.getLastUpdatedTimestamp());
                        array.put(json);
                    }
                } finally {
                    buffer.release();
                }
                return array.toString();
            }
        });
    }

    public static void submitScore(String id, double score, String tag, HaxeObject callback) {
        if (!ready(callback))
            return;

        LeaderboardsClient client = PlayGames.getLeaderboardsClient(activity());
        long value = (long) score;
        boolean hasTag = tag != null && !tag.isEmpty();

        if (callback == null) {
            if (hasTag)
                client.submitScore(id, value, tag);
            else
                client.submitScore(id, value);
            return;
        }

        Task<ScoreSubmissionData> task = hasTag ? client.submitScoreImmediate(id, value, tag) : client.submitScoreImmediate(id, value);
        onTask(task, callback, new Converter<ScoreSubmissionData>() {
            @Override
            public String convert(ScoreSubmissionData data) throws Exception {
                JSONObject json = new JSONObject();
                json.put("leaderboardId", data.getLeaderboardId());
                json.put("playerId", data.getPlayerId());
                return json.toString();
            }
        });
    }

    public static void showLeaderboard(String id, int timeSpan, int collection, int requestCode, HaxeObject callback) {
        if (!ready(callback))
            return;

        LeaderboardsClient client = PlayGames.getLeaderboardsClient(activity());
        Task<Intent> task = (id == null || id.isEmpty())
            ? client.getAllLeaderboardsIntent()
            : client.getLeaderboardIntent(id, timeSpan, collection);
        startIntent(task, requestCode, callback);
    }

    public static void loadPlayerScore(String id, int timeSpan, int collection, HaxeObject callback) {
        if (!ready(callback))
            return;

        onTask(PlayGames.getLeaderboardsClient(activity()).loadCurrentPlayerLeaderboardScore(id, timeSpan, collection), callback,
            new Converter<AnnotatedData<LeaderboardScore>>() {
                @Override
                public String convert(AnnotatedData<LeaderboardScore> value) throws Exception {
                    LeaderboardScore score = value.get();
                    if (score == null)
                        return "null";

                    JSONObject json = new JSONObject();
                    json.put("score", (double) score.getRawScore());
                    json.put("displayScore", score.getDisplayScore());
                    json.put("rank", (double) score.getRank());
                    json.put("displayRank", score.getDisplayRank());
                    json.put("tag", score.getScoreTag() != null ? score.getScoreTag() : "");
                    json.put("timestamp", (double) score.getTimestampMillis());
                    return json.toString();
                }
            });
    }

    public static void incrementEvent(String id, int amount) {
        if (activity() == null)
            return;

        init();
        PlayGames.getEventsClient(activity()).increment(id, amount);
    }

    public static void setPopupGravity(final int gravity) {
        final Activity act = activity();
        if (act == null)
            return;

        init();
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    View view = Extension.mainView != null ? Extension.mainView : act.getWindow().getDecorView();
                    Object client = PlayGames.class.getMethod("getGamesClient", Activity.class).invoke(null, act);
                    if (client != null) {
                        client.getClass().getMethod("setViewForPopups", View.class).invoke(client, view);
                        client.getClass().getMethod("setGravityForPopups", int.class).invoke(client, gravity);
                    }
                } catch (NoSuchMethodException e) {
                    Log.i(TAG, "Popup gravity is handled by Play Games on this SDK version");
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        });
    }

    private interface SnapshotAction {
        void run(Snapshot snapshot, SnapshotsClient client, HaxeObject callback) throws Exception;
    }

    private static void openSnapshot(final String name, final boolean create, final HaxeObject callback, final SnapshotAction action) {
        if (!ready(callback))
            return;

        final SnapshotsClient client = PlayGames.getSnapshotsClient(activity());
        client.open(name, create, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
            .addOnCompleteListener(new OnCompleteListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
                @Override
                public void onComplete(Task<SnapshotsClient.DataOrConflict<Snapshot>> task) {
                    if (!task.isSuccessful()) {
                        respond(callback, false, error(task.getException()));
                        return;
                    }

                    SnapshotsClient.DataOrConflict<Snapshot> result = task.getResult();
                    Snapshot snapshot = result.isConflict() ? result.getConflict().getSnapshot() : result.getData();
                    if (snapshot == null) {
                        respond(callback, false, "{\"code\":-1,\"message\":\"Snapshot not found\"}");
                        return;
                    }

                    try {
                        action.run(snapshot, client, callback);
                    } catch (Exception e) {
                        client.discardAndClose(snapshot);
                        respond(callback, false, error(e));
                    }
                }
            });
    }

    public static void saveGame(String name, final String data, final String description, final double playedTimeMillis, final HaxeObject callback) {
        openSnapshot(name, true, callback, new SnapshotAction() {
            @Override
            public void run(Snapshot snapshot, SnapshotsClient client, HaxeObject cb) throws Exception {
                snapshot.getSnapshotContents().writeBytes((data == null ? "" : data).getBytes("UTF-8"));

                SnapshotMetadataChange.Builder builder = new SnapshotMetadataChange.Builder();
                if (description != null && !description.isEmpty())
                    builder.setDescription(description);
                if (playedTimeMillis >= 0)
                    builder.setPlayedTimeMillis((long) playedTimeMillis);

                onTask(client.commitAndClose(snapshot, builder.build()), cb, new Converter<SnapshotMetadata>() {
                    @Override
                    public String convert(SnapshotMetadata value) throws Exception {
                        return metadataToJson(value).toString();
                    }
                });
            }
        });
    }

    public static void loadGame(String name, final HaxeObject callback) {
        openSnapshot(name, false, callback, new SnapshotAction() {
            @Override
            public void run(Snapshot snapshot, SnapshotsClient client, HaxeObject cb) throws Exception {
                byte[] bytes = snapshot.getSnapshotContents().readFully();
                client.discardAndClose(snapshot);
                respond(cb, true, new String(bytes, "UTF-8"));
            }
        });
    }

    public static void deleteGame(String name, final HaxeObject callback) {
        openSnapshot(name, false, callback, new SnapshotAction() {
            @Override
            public void run(Snapshot snapshot, SnapshotsClient client, HaxeObject cb) throws Exception {
                SnapshotMetadata metadata = snapshot.getMetadata().freeze();
                client.discardAndClose(snapshot);
                onTask(client.delete(metadata), cb, new Converter<String>() {
                    @Override
                    public String convert(String value) {
                        return value;
                    }
                });
            }
        });
    }

    private static JSONObject metadataToJson(SnapshotMetadata metadata) throws Exception {
        JSONObject json = new JSONObject();
        json.put("name", metadata.getUniqueName());
        json.put("title", metadata.getUniqueName() != null ? metadata.getUniqueName() : "");
        json.put("description", metadata.getDescription() != null ? metadata.getDescription() : "");
        json.put("lastModified", (double) metadata.getLastModifiedTimestamp());
        json.put("playedTime", (double) metadata.getPlayedTime());
        return json;
    }

    public static void listSavedGames(boolean forceReload, HaxeObject callback) {
        if (!ready(callback))
            return;

        onTask(PlayGames.getSnapshotsClient(activity()).load(forceReload), callback, new Converter<AnnotatedData<SnapshotMetadataBuffer>>() {
            @Override
            public String convert(AnnotatedData<SnapshotMetadataBuffer> value) throws Exception {
                JSONArray array = new JSONArray();
                SnapshotMetadataBuffer buffer = value.get();
                if (buffer == null)
                    return array.toString();

                try {
                    for (int i = 0; i < buffer.getCount(); i++)
                        array.put(metadataToJson(buffer.get(i)));
                } finally {
                    buffer.release();
                }
                return array.toString();
            }
        });
    }

    public static void showSavedGames(String title, boolean allowAdd, boolean allowDelete, int maxSnapshots, int requestCode, HaxeObject callback) {
        if (!ready(callback))
            return;

        startIntent(PlayGames.getSnapshotsClient(activity()).getSelectSnapshotIntent(title, allowAdd, allowDelete, maxSnapshots), requestCode, callback);
    }
}
