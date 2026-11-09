package com.example.retaildpc.demo;

import java.util.Locale;

/**
 * After final Experience profile CONFIRM, acknowledge a visible Retail Mode
 * downloading-in-progress message. A fixed wait or a generic launcher view is
 * NOT evidence of successful demo start. This is a UI heuristic, not an API.
 */
final class DemoCompletionWatch {
    private static final String TARGET = "com.samsung.rms.retailagent.global";

    void reset() { }

    boolean update(long confirmedAt, String eventPackage, String visibleText) {
        return confirmedAt > 0L && TARGET.equals(eventPackage) &&
            isDownloadProgressText(visibleText);
    }

    static boolean isDownloadProgressText(String text) {
        if (text == null) return false;
        String s = text.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
        if (s.isEmpty() || s.length() > 240) return false;
        // Reject failure/cancel messages; never confuse them with success.
        if (s.contains("failed") || s.contains("failure") || s.contains("error") ||
            s.contains("cancel") || s.contains("unable") || s.contains("cannot") ||
            s.contains("błąd") || s.contains("anulow") || s.contains("przerwan") ||
            s.contains("nie można") || s.contains("nie uda") || s.contains("nie powiod") || s.contains("niepowod")) return false;
        // Keep exact progress phrases instead of accepting a generic Download button.
        return s.contains("downloading") || s.contains("download in progress") ||
            s.contains("download started") || s.contains("starting download") ||
            s.contains("pobieranie") || s.contains("pobieram") ||
            s.contains("rozpoczęto pobieranie") || s.contains("rozpoczynam pobieranie");
    }
}
