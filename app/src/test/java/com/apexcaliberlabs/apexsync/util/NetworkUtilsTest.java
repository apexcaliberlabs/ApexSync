package com.apexcaliberlabs.apexsync.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link NetworkUtils}.
 */
public class NetworkUtilsTest {

    @Test
    public void formatIpAddress_returnsCorrectDottedDecimal() {
        // 0xC0A82B01 == 192.168.43.1 stored in little-endian int
        // int value: (1 << 0) | (43 << 8) | (168 << 16) | (192 << 24)
        int ip = 1 | (43 << 8) | (168 << 16) | (192 << 24);
        String result = NetworkUtils.formatIpAddress(ip);
        assertEquals("192.168.43.1", result);
    }

    @Test
    public void formatIpAddress_handles_zeros() {
        assertEquals("0.0.0.0", NetworkUtils.formatIpAddress(0));
    }

    @Test
    public void formatIpAddress_handles_loopback() {
        // 127.0.0.1: 127 | (0 << 8) | (0 << 16) | (1 << 24)
        // In little-endian: (1 << 0) | (0 << 8) | (0 << 16) | (127 << 24)
        int ip = 1 | (127 << 24);
        String result = NetworkUtils.formatIpAddress(ip);
        assertEquals("1.0.0.127", result);
    }
}
