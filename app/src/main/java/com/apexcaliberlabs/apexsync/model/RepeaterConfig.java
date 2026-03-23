package com.apexcaliberlabs.apexsync.model;

/**
 * Configuration details for the Wi-Fi Direct repeater group.
 * <p>
 * Populated from the {@link android.net.wifi.p2p.WifiP2pGroup} returned by
 * {@link android.net.wifi.p2p.WifiP2pManager} group info callbacks.
 */
public class RepeaterConfig {

    private final String networkName;
    private final String passphrase;
    private final String ownerAddress;

    public RepeaterConfig(String networkName, String passphrase, String ownerAddress) {
        this.networkName = networkName;
        this.passphrase = passphrase;
        this.ownerAddress = ownerAddress;
    }

    public String getNetworkName() {
        return networkName;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public String getOwnerAddress() {
        return ownerAddress;
    }

    /**
     * Returns a Wi-Fi network URI suitable for encoding as a QR code.
     * Format: {@code WIFI:S:<SSID>;T:WPA;P:<password>;;}
     */
    public String toWifiQrString() {
        return "WIFI:S:" + escapeQrField(networkName)
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
        return "RepeaterConfig{networkName='" + networkName
                + "', ownerAddress='" + ownerAddress + "'}";
    }
}
