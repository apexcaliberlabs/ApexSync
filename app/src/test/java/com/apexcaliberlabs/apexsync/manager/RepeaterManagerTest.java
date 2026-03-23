package com.apexcaliberlabs.apexsync.manager;

import com.apexcaliberlabs.apexsync.model.RepeaterConfig;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link RepeaterConfig} (model used by {@link RepeaterManager}).
 */
public class RepeaterManagerTest {

    @Test
    public void repeaterConfig_toWifiQrString_formatsCorrectly() {
        RepeaterConfig config = new RepeaterConfig("DIRECT-xy-ApexSync", "passphrase1", "AA:BB:CC:DD:EE:FF");
        String qr = config.toWifiQrString();
        assertTrue(qr.contains("S:DIRECT-xy-ApexSync"));
        assertTrue(qr.contains("P:passphrase1"));
        assertTrue(qr.contains("T:WPA"));
    }

    @Test
    public void repeaterConfig_toWifiQrString_handlesNullValues() {
        RepeaterConfig config = new RepeaterConfig(null, null, null);
        String qr = config.toWifiQrString();
        // Should not throw and should return a valid (empty) QR string
        assertNotNull(qr);
        assertTrue(qr.startsWith("WIFI:"));
    }

    @Test
    public void repeaterConfig_getters_returnExpectedValues() {
        RepeaterConfig config = new RepeaterConfig("TestNet", "TestPass", "11:22:33:44:55:66");
        assertEquals("TestNet", config.getNetworkName());
        assertEquals("TestPass", config.getPassphrase());
        assertEquals("11:22:33:44:55:66", config.getOwnerAddress());
    }
}
