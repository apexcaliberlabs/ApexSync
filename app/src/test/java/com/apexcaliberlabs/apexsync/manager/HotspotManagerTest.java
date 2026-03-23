package com.apexcaliberlabs.apexsync.manager;

import com.apexcaliberlabs.apexsync.model.HotspotConfig;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link HotspotConfig} (model used by {@link HotspotManager}).
 */
public class HotspotManagerTest {

    @Test
    public void hotspotConfig_toWifiQrString_formatsCorrectly() {
        HotspotConfig config = new HotspotConfig("MyHotspot", "secret123", 0);
        String qr = config.toWifiQrString();
        assertTrue(qr.contains("S:MyHotspot"));
        assertTrue(qr.contains("P:secret123"));
        assertTrue(qr.contains("T:WPA"));
    }

    @Test
    public void hotspotConfig_toWifiQrString_escapesSemicolon() {
        HotspotConfig config = new HotspotConfig("Net;work", "pass;word", 0);
        String qr = config.toWifiQrString();
        assertTrue(qr.contains("S:Net\\;work"));
        assertTrue(qr.contains("P:pass\\;word"));
    }

    @Test
    public void hotspotConfig_toWifiQrString_escapesBackslash() {
        HotspotConfig config = new HotspotConfig("My\\Net", "pa\\ss", 0);
        String qr = config.toWifiQrString();
        assertTrue(qr.contains("S:My\\\\Net"));
        assertTrue(qr.contains("P:pa\\\\ss"));
    }

    @Test
    public void hotspotConfig_getters_returnExpectedValues() {
        HotspotConfig config = new HotspotConfig("TestSSID", "TestPass", 1);
        assertEquals("TestSSID", config.getSsid());
        assertEquals("TestPass", config.getPassphrase());
        assertEquals(1, config.getBand());
    }
}
