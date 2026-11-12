package java.androidmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;
import org.haxe.lime.HaxeObject;
import org.json.JSONObject;

public class Network {

    private static HaxeObject listener = null;
    private static ConnectivityManager.NetworkCallback networkCallback = null;
    private static BroadcastReceiver receiver = null;
    private static String lastStatus = null;

    private static ConnectivityManager manager() {
        Context ctx = Utils.context();
        return ctx != null ? (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE) : null;
    }

    private static NetworkCapabilities capabilities() {
        ConnectivityManager cm = manager();
        if (cm == null || Build.VERSION.SDK_INT < 23)
            return null;

        try {
            android.net.Network active = cm.getActiveNetwork();
            return active != null ? cm.getNetworkCapabilities(active) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    public static boolean isConnected() {
        ConnectivityManager cm = manager();
        if (cm == null)
            return false;

        try {
            if (Build.VERSION.SDK_INT >= 23) {
                NetworkCapabilities caps = capabilities();
                return caps != null
                    && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            }

            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    public static String getConnectionType() {
        ConnectivityManager cm = manager();
        if (cm == null)
            return "none";

        try {
            if (Build.VERSION.SDK_INT >= 23) {
                NetworkCapabilities caps = capabilities();
                if (caps == null)
                    return "none";

                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
                    return "vpn";
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                    return "wifi";
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                    return "cellular";
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
                    return "ethernet";
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH))
                    return "bluetooth";
                return "other";
            }

            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info == null || !info.isConnected())
                return "none";

            switch (info.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    return "wifi";
                case ConnectivityManager.TYPE_MOBILE:
                    return "cellular";
                case ConnectivityManager.TYPE_ETHERNET:
                    return "ethernet";
                case ConnectivityManager.TYPE_BLUETOOTH:
                    return "bluetooth";
                case ConnectivityManager.TYPE_VPN:
                    return "vpn";
                default:
                    return "other";
            }
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
            return "none";
        }
    }

    public static boolean isMetered() {
        ConnectivityManager cm = manager();
        return cm != null && cm.isActiveNetworkMetered();
    }

    public static int getDownstreamKbps() {
        NetworkCapabilities caps = capabilities();
        return caps != null ? caps.getLinkDownstreamBandwidthKbps() : -1;
    }

    public static int getUpstreamKbps() {
        NetworkCapabilities caps = capabilities();
        return caps != null ? caps.getLinkUpstreamBandwidthKbps() : -1;
    }

    public static String getStatus() {
        JSONObject json = new JSONObject();
        try {
            json.put("connected", isConnected());
            json.put("type", getConnectionType());
            json.put("metered", isMetered());
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
        return json.toString();
    }

    private static void notifyListener() {
        HaxeObject target = listener;
        if (target == null)
            return;

        String status = getStatus();
        if (status.equals(lastStatus))
            return;

        lastStatus = status;

        try {
            target.call("onNetworkChanged", new Object[]{ status });
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    @SuppressWarnings("deprecation")
    public static void startListening(HaxeObject object) {
        listener = object;
        ConnectivityManager cm = manager();
        if (cm == null || networkCallback != null || receiver != null)
            return;

        try {
            if (Build.VERSION.SDK_INT >= 24) {
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(android.net.Network network) {
                        notifyListener();
                    }

                    @Override
                    public void onLost(android.net.Network network) {
                        notifyListener();
                    }

                    @Override
                    public void onCapabilitiesChanged(android.net.Network network, NetworkCapabilities caps) {
                        notifyListener();
                    }
                };
                cm.registerDefaultNetworkCallback(networkCallback);
            } else {
                receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        notifyListener();
                    }
                };
                Utils.context().registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            }
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }
    }

    public static void stopListening() {
        ConnectivityManager cm = manager();
        try {
            if (networkCallback != null && cm != null)
                cm.unregisterNetworkCallback(networkCallback);

            if (receiver != null && Utils.context() != null)
                Utils.context().unregisterReceiver(receiver);
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG, e.toString());
        }

        networkCallback = null;
        receiver = null;
        listener = null;
        lastStatus = null;
    }
}
