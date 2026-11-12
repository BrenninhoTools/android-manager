package androidmanager.tools;

enum abstract Permission(String) from String to String {
	var READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
	var WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
	var MANAGE_EXTERNAL_STORAGE = "android.permission.MANAGE_EXTERNAL_STORAGE";
	var MANAGE_MEDIA = "android.permission.MANAGE_MEDIA";
	var READ_MEDIA_IMAGES = "android.permission.READ_MEDIA_IMAGES";
	var READ_MEDIA_VIDEO = "android.permission.READ_MEDIA_VIDEO";
	var READ_MEDIA_AUDIO = "android.permission.READ_MEDIA_AUDIO";
	var READ_MEDIA_VISUAL_USER_SELECTED = "android.permission.READ_MEDIA_VISUAL_USER_SELECTED";
	var POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";
	var CAMERA = "android.permission.CAMERA";
	var RECORD_AUDIO = "android.permission.RECORD_AUDIO";
	var VIBRATE = "android.permission.VIBRATE";
	var INTERNET = "android.permission.INTERNET";
	var ACCESS_NETWORK_STATE = "android.permission.ACCESS_NETWORK_STATE";
	var ACCESS_WIFI_STATE = "android.permission.ACCESS_WIFI_STATE";
	var ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION";
	var ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";
	var BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT";
	var BLUETOOTH_SCAN = "android.permission.BLUETOOTH_SCAN";
	var READ_PHONE_STATE = "android.permission.READ_PHONE_STATE";
	var WAKE_LOCK = "android.permission.WAKE_LOCK";
	var SYSTEM_ALERT_WINDOW = "android.permission.SYSTEM_ALERT_WINDOW";
	var WRITE_SETTINGS = "android.permission.WRITE_SETTINGS";
	var REQUEST_INSTALL_PACKAGES = "android.permission.REQUEST_INSTALL_PACKAGES";
}
