package com.apexcaliberlabs.apexsync.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class that centralises runtime permission checks and requests.
 */
public final class PermissionHelper {

    /** Request code for hotspot-related permissions. */
    public static final int REQUEST_CODE_HOTSPOT = 1001;

    /** Request code for repeater-related permissions. */
    public static final int REQUEST_CODE_REPEATER = 1002;

    /** Request code for notification permission (API 33+). */
    public static final int REQUEST_CODE_NOTIFICATION = 1003;

    private PermissionHelper() {
        // Utility class – no instantiation.
    }

    /**
     * Returns the list of permissions required to start the local-only hotspot.
     */
    @NonNull
    public static String[] getHotspotPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE);
        permissions.add(Manifest.permission.CHANGE_WIFI_STATE);
        return permissions.toArray(new String[0]);
    }

    /**
     * Returns the list of permissions required to start the Wi-Fi Direct repeater.
     * On API 33+ {@link Manifest.permission#NEARBY_WIFI_DEVICES} replaces the location
     * permission for Wi-Fi P2P operations.
     */
    @NonNull
    public static String[] getRepeaterPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE);
        permissions.add(Manifest.permission.CHANGE_WIFI_STATE);
        return permissions.toArray(new String[0]);
    }

    /**
     * Returns {@code true} if all of the supplied {@code permissions} have been granted.
     */
    public static boolean hasPermissions(@NonNull Context context,
                                         @NonNull String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Requests the given {@code permissions} from within a {@link Fragment}.
     *
     * @param fragment    the requesting fragment
     * @param permissions the permissions to request
     * @param requestCode the request code for {@code onRequestPermissionsResult}
     */
    public static void requestPermissions(@NonNull Fragment fragment,
                                          @NonNull String[] permissions,
                                          int requestCode) {
        fragment.requestPermissions(permissions, requestCode);
    }

    /**
     * Requests the given {@code permissions} from within an {@link Activity}.
     *
     * @param activity    the requesting activity
     * @param permissions the permissions to request
     * @param requestCode the request code for {@code onRequestPermissionsResult}
     */
    public static void requestPermissions(@NonNull Activity activity,
                                          @NonNull String[] permissions,
                                          int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    /**
     * Returns {@code true} if the app should show a rationale for any of the supplied
     * permissions.
     */
    public static boolean shouldShowRationale(@NonNull Activity activity,
                                              @NonNull String... permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if all entries in {@code grantResults} are
     * {@link PackageManager#PERMISSION_GRANTED}.
     */
    public static boolean allGranted(@NonNull int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) return false;
        }
        return grantResults.length > 0;
    }
}
