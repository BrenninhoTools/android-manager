package androidmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.haxe.lime.HaxeObject;
import org.json.JSONArray;

public class Interface {

    private static final int THEME = android.R.style.Theme_DeviceDefault_Dialog_Alert;

    private static AlertDialog progressDialog = null;
    private static ProgressBar progressBar = null;
    private static TextView progressText = null;

    private static void call(HaxeObject target, String method, Object... args) {
        if (target == null)
            return;

        try {
            target.call(method, args);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    private static int dp(float value) {
        return (int) (value * Utils.activity().getResources().getDisplayMetrics().density);
    }

    public static void showToast(final String msg) {
        showStyledToast(msg, false, 0xFFFAFAFA, 0xFF111111, true, 64);
    }

    @SuppressWarnings("deprecation")
    public static void showStyledToast(final String msg, final boolean longDuration, final int backgroundColor,
            final int textColor, final boolean showIcon, final int bottomOffset) {
        final Activity act = Utils.activity();
        if (act == null)
            return;

        act.runOnUiThread(() -> {
            try {
                LinearLayout layout = new LinearLayout(act);
                layout.setOrientation(LinearLayout.HORIZONTAL);
                layout.setGravity(Gravity.CENTER_VERTICAL);

                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.RECTANGLE);
                shape.setCornerRadius(100f);
                shape.setColor(backgroundColor);
                layout.setBackground(shape);
                layout.setPadding(dp(20), dp(14), dp(20), dp(14));

                if (showIcon) {
                    ImageView icon = new ImageView(act);
                    Drawable appIcon = act.getPackageManager().getApplicationIcon(act.getApplicationInfo());
                    icon.setImageDrawable(appIcon);

                    LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
                    iconParams.setMargins(0, 0, dp(12), 0);
                    icon.setLayoutParams(iconParams);
                    layout.addView(icon);
                }

                TextView text = new TextView(act);
                text.setText(msg);
                text.setTextColor(textColor);
                text.setTextSize(16f);
                layout.addView(text);

                Toast toast = new Toast(act);
                toast.setDuration(longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, dp(bottomOffset));
                toast.show();
            } catch (Exception e) {
                Toast.makeText(act, msg, longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void makeToast(final String msg, final int duration, final int gravity, final int xOffset, final int yOffset) {
        final Activity act = Utils.activity();
        if (act == null)
            return;

        act.runOnUiThread(() -> {
            Toast toast = Toast.makeText(act, msg, duration == 1 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
            if (gravity >= 0)
                toast.setGravity(gravity, xOffset, yOffset);
            toast.show();
        });
    }

    public static void alert(final String title, final String msg, final String btn, final HaxeObject callback) {
        final Activity act = Utils.activity();
        if (act == null)
            return;

        act.runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(act, THEME);
            builder.setTitle(title);
            builder.setMessage(msg);
            builder.setPositiveButton(btn, (dialog, which) -> call(callback, "onTrigger"));
            builder.setCancelable(false);
            builder.show();
        });
    }

    public static void confirm(final String title, final String msg, final String yes, final String no, final HaxeObject onYes, final HaxeObject onNo) {
        final Activity act = Utils.activity();
        if (act == null)
            return;

        act.runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(act, THEME);
            builder.setTitle(title);
            builder.setMessage(msg);
            builder.setPositiveButton(yes, (dialog, which) -> call(onYes, "onTrigger"));
            builder.setNegativeButton(no, (dialog, which) -> call(onNo, "onTrigger"));
            builder.setCancelable(false);
            builder.show();
        });
    }

    public static void dialog(final String title, final String msg, final String positive, final String negative,
            final String neutral, final boolean cancelable, final HaxeObject listener) {
        final Activity act = Utils.activity();
        if (act == null)
            return;

        act.runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(act, THEME);
            builder.setTitle(title);
            builder.setMessage(msg);

            if (positive != null && !positive.isEmpty())
                builder.setPositiveButton(positive, (d, which) -> call(listener, "onResult", 1));
            if (negative != null && !negative.isEmpty())
                builder.setNegativeButton(negative, (d, which) -> call(listener, "onResult", 2));
            if (neutral != null && !neutral.isEmpty())
                builder.setNeutralButton(neutral, (d, which) -> call(listener, "onResult", 3));

            builder.setCancelable(cancelable);
            builder.setOnCancelListener((d) -> call(listener, "onResult", 0));
            builder.show();
        });
    }

    public static void input(final String title, final String msg, final String hint, final String defaultText,
            final int inputType, final String positive, final String negative, final HaxeObject listener) {
        final Activity act = Utils.activity();
        if (act == null)
            return;

        act.runOnUiThread(() -> {
            final EditText field = new EditText(act);
            field.setHint(hint);
            field.setText(defaultText);
            field.setSelectAllOnFocus(true);
            field.setSingleLine(inputType != 2);

            switch (inputType) {
                case 1:
                    field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    break;
                case 2:
                    field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                    break;
                case 3:
                    field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    break;
                case 4:
                    field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                    break;
                case 5:
                    field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
                    break;
                default:
                    field.setInputType(InputType.TYPE_CLASS_TEXT);
                    break;
            }

            FrameLayout container = new FrameLayout(act);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            params.leftMargin = dp(20);
            params.rightMargin = dp(20);
            field.setLayoutParams(params);
            container.addView(field);

            AlertDialog.Builder builder = new AlertDialog.Builder(act, THEME);
            builder.setTitle(title);
            if (msg != null && !msg.isEmpty())
                builder.setMessage(msg);
            builder.setView(container);
            builder.setPositiveButton(positive, (d, which) -> call(listener, "onInput", true, field.getText().toString()));
            builder.setNegativeButton(negative, (d, which) -> call(listener, "onInput", false, ""));
            builder.setOnCancelListener((d) -> call(listener, "onInput", false, ""));
            builder.show();
            field.requestFocus();
        });
    }

    public static void list(final String title, final String[] items, final HaxeObject listener) {
        final Activity act = Utils.activity();
        if (act == null || items == null)
            return;

        act.runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(act, THEME);
            builder.setTitle(title);
            builder.setItems(items, (d, which) -> call(listener, "onSelect", which));
            builder.setOnCancelListener((d) -> call(listener, "onSelect", -1));
            builder.show();
        });
    }

    public static void singleChoice(final String title, final String[] items, final int checked,
            final String positive, final String negative, final HaxeObject listener) {
        final Activity act = Utils.activity();
        if (act == null || items == null)
            return;

        act.runOnUiThread(() -> {
            final int[] selected = { checked };
            AlertDialog.Builder builder = new AlertDialog.Builder(act, THEME);
            builder.setTitle(title);
            builder.setSingleChoiceItems(items, checked, (d, which) -> selected[0] = which);
            builder.setPositiveButton(positive, (d, which) -> call(listener, "onSelect", selected[0]));
            builder.setNegativeButton(negative, (d, which) -> call(listener, "onSelect", -1));
            builder.setOnCancelListener((d) -> call(listener, "onSelect", -1));
            builder.show();
        });
    }

    public static void multiChoice(final String title, final String[] items, final String checkedJson,
            final String positive, final String negative, final HaxeObject listener) {
        final Activity act = Utils.activity();
        if (act == null || items == null)
            return;

        act.runOnUiThread(() -> {
            final boolean[] checked = new boolean[items.length];
            try {
                JSONArray initial = new JSONArray(checkedJson == null || checkedJson.isEmpty() ? "[]" : checkedJson);
                for (int i = 0; i < initial.length(); i++) {
                    int index = initial.getInt(i);
                    if (index >= 0 && index < checked.length)
                        checked[index] = true;
                }
            } catch (Exception ignored) {
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(act, THEME);
            builder.setTitle(title);
            builder.setMultiChoiceItems(items, checked, (d, which, isChecked) -> checked[which] = isChecked);
            builder.setPositiveButton(positive, (d, which) -> {
                JSONArray result = new JSONArray();
                for (int i = 0; i < checked.length; i++) {
                    if (checked[i])
                        result.put(i);
                }
                call(listener, "onMultiSelect", true, result.toString());
            });
            builder.setNegativeButton(negative, (d, which) -> call(listener, "onMultiSelect", false, "[]"));
            builder.setOnCancelListener((d) -> call(listener, "onMultiSelect", false, "[]"));
            builder.show();
        });
    }

    public static void showProgress(final String title, final String msg, final boolean indeterminate, final int max) {
        final Activity act = Utils.activity();
        if (act == null)
            return;

        act.runOnUiThread(() -> {
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

            LinearLayout layout = new LinearLayout(act);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(dp(24), dp(16), dp(24), dp(8));

            progressText = new TextView(act);
            progressText.setText(msg);
            layout.addView(progressText);

            progressBar = new ProgressBar(act, null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setIndeterminate(indeterminate);
            progressBar.setMax(Math.max(1, max));
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            barParams.topMargin = dp(12);
            progressBar.setLayoutParams(barParams);
            layout.addView(progressBar);

            AlertDialog.Builder builder = new AlertDialog.Builder(act, THEME);
            builder.setTitle(title);
            builder.setView(layout);
            builder.setCancelable(false);
            progressDialog = builder.show();
        });
    }

    public static void updateProgress(final int value, final String msg) {
        Utils.runOnUi(() -> {
            if (progressBar != null) {
                progressBar.setIndeterminate(false);
                progressBar.setProgress(value);
            }

            if (progressText != null && msg != null && !msg.isEmpty())
                progressText.setText(msg);
        });
    }

    public static void hideProgress() {
        Utils.runOnUi(() -> {
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

            progressDialog = null;
            progressBar = null;
            progressText = null;
        });
    }

    public static void navigate(final String action, final int code) {
        final Activity act = Utils.activity();
        if (act == null)
            return;

        act.runOnUiThread(() -> {
            try {
                Intent intent = new Intent(action);
                if (action.equals(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    || action.equals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    || action.equals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    || action.equals(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    || action.equals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES))
                    intent.setData(Utils.packageUri());

                act.startActivityForResult(intent, code);
            } catch (Exception e) {
                act.startActivityForResult(new Intent(Settings.ACTION_SETTINGS), code);
            }
        });
    }
}
