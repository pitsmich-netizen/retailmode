package com.example.retaildpc;

import java.util.Arrays;
/**
 * Volatile test credential: only in the current app process; never saved to disk or logs.
 * Android can kill the process, in which case the test must stop safely.
 */
final class SecretMemory {
    private static char[] secret;
    private SecretMemory() { }
    static synchronized void set(char[] value) {
        clear();
        if (value != null && value.length > 0) secret = Arrays.copyOf(value, value.length);
    }
    static synchronized boolean available() { return secret != null && secret.length > 0; }
    static synchronized String takeForInput() {
        if (!available()) return null;
        String value = new String(secret);
        clear();
        return value;
    }
    static synchronized void clear() {
        if (secret != null) Arrays.fill(secret, '\0');
        secret = null;
    }
}
