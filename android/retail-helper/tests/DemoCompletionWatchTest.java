package com.example.retailhelper;

public final class DemoCompletionWatchTest {
    private static void verify(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        DemoCompletionWatch w = new DemoCompletionWatch();
        long confirm = 100000L;
        String pkg = "com.samsung.rms.retailagent.global";
        verify(!w.update(0, pkg, "Downloading demo"), "must be after profile CONFIRM");
        verify(!w.update(confirm, "com.android.settings", "Downloading demo"), "foreign package");
        verify(!w.update(confirm, "com.sec.android.app.launcher", "Downloading demo"), "launcher is not proof");
        verify(!w.update(confirm, pkg, "CONFIRM"), "confirm is not proof");
        verify(!w.update(confirm, pkg, "Download"), "download button is not progress");
        verify(!w.update(confirm, pkg, "Download failed"), "failure cannot trigger success");
        verify(!w.update(confirm, pkg, "Pobieranie nie powiodło się"), "localized error");
        verify(w.update(confirm, pkg, "Downloading demo files…"), "english progress");
        verify(w.update(confirm, pkg, "Pobieranie zawartości demo"), "polish progress");
        verify(w.update(confirm, pkg, "Download in progress"), "progress phrase");
        verify(w.update(confirm, pkg, "Rozpoczęto pobieranie danych"), "start phrase");
        System.out.println("DemoCompletionWatch 0.2.3 tests OK");
    }
}
