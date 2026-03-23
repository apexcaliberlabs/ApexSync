package com.apexcaliberlabs.apexsync.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

/**
 * Utility methods for querying network state and formatting network addresses.
 */
public final class NetworkUtils {

    private NetworkUtils() {
        // Utility class – no instantiation.
    }

    /**
     * Returns {@code true} if the device is currently connected to a Wi-Fi network.
     */
    public static boolean isWifiConnected(@NonNull Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
        return capabilities != null
                && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    /**
     * Returns {@code true} if the device has any active internet connectivity.
     */
    public static boolean isInternetAvailable(@NonNull Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /**
     * Returns the IPv4 address of the local-only hotspot interface, or {@code null} if
     * no matching interface is found.
     *
     * @param interfaceName the name of the network interface (e.g. "wlan1", "p2p-wlan0-0")
     */
    @Nullable
    public static String getInterfaceIpAddress(@NonNull String interfaceName) {
        try {
            List<NetworkInterface> interfaces =
                    Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface iface : interfaces) {
                if (!iface.getName().equalsIgnoreCase(interfaceName)) continue;
                List<InetAddress> addresses = Collections.list(iface.getInetAddresses());
                for (InetAddress addr : addresses) {
                    if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Converts a raw integer IP address (as returned by older Wi-Fi APIs) into a
     * human-readable dotted-decimal string. The integer is treated as little-endian,
     * meaning the least significant byte is the first octet of the address (as
     * returned by {@link android.net.wifi.WifiInfo#getIpAddress()}).
     *
     * @param ipInt the little-endian integer IP address
     * @return dotted-decimal string, e.g. "192.168.43.1"
     */
    @NonNull
    public static String formatIpAddress(int ipInt) {
        return (ipInt & 0xFF) + "."
                + ((ipInt >> 8) & 0xFF) + "."
                + ((ipInt >> 16) & 0xFF) + "."
                + ((ipInt >> 24) & 0xFF);
    }
}
