package com.unireview.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IpHashUtilTest {

    @Test
    void hash_isDeterministic() {
        assertEquals(IpHashUtil.hash("192.168.1.1"), IpHashUtil.hash("192.168.1.1"));
    }

    @Test
    void hash_neverEqualsRawInput() {
        String rawIp = "203.0.113.42";
        String hashed = IpHashUtil.hash(rawIp);
        assertNotEquals(rawIp, hashed);
        assertEquals(64, hashed.length()); // SHA-256 hex digest length
    }

    @Test
    void hash_differentIpsProduceDifferentHashes() {
        assertNotEquals(IpHashUtil.hash("1.1.1.1"), IpHashUtil.hash("2.2.2.2"));
    }

    @Test
    void hash_nullOrBlankReturnsNull() {
        assertNull(IpHashUtil.hash(null));
        assertNull(IpHashUtil.hash("  "));
    }
}
