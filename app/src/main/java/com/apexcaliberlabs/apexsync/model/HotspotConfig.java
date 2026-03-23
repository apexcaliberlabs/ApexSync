package com.apexcaliberlabs.apexsync.model;

/**
 * Configuration details for the local-only hotspot.
 * <p>
 * On API 29+ the OS assigns SSID and passphrase automatically.
 * This object is populated from the {@link android.net.wifi.WifiConfiguration}
 * returned by {@link android.net.wifi.WifiManager.LocalOnlyHotspotReservation}.
 */
public class HotspotConfig {

    private final String ssid;
    private final String passphrase;
    private final int band; // WifiConfiguration.AP_BAND_* constant

    public HotspotConfig(String ssid, String passphrase, int band) {
        this.ssid = ssid;
        this.passphrase = passphrase;
        this.band = band;
    }

    public String getSsid() {
        return ssid;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public int getBand() {
        return band;
    }

    /**
     * Returns a Wi-Fi network URI suitable for encoding as a QR code.
     * Format: {@code WIFI:S:<SSID>;T:WPA;P:<password>;;}
     */
    public String toWifiQrString() {
        return "WIFI:S:" + escapeQrField(ssid)
                + ";T:WPA;P:" + escapeQrField(passphrase)
                + ";;";
    }

    private static String escapeQrField(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace(";", "\\;")
                    .replace(",", "\\,")
                    .replace("\"", "\\\"");
    }

    @Override
    public String toString() {
        return "HotspotConfig{ssid='" + ssid + "', band=" + band + "}";
    }
}
