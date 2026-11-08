package com.example.retaildpc;

public final class Constants {
    private Constants() {}
    public static final String DEMO_URL =
        "https://github.com/pitsmich-netizen/retailmode/releases/download/rmtest/RetailMode.apk";
    // Weryfikacja SHA-256 z metadanych GitHub Release, rmtest, 2026-11-04.
    public static final String DEMO_SHA256 =
        "da5ede9aa02aa278fa82b97caba8eb2e79a4e6fea9bdc5e3b50ebb3e5bc245c4";
    public static final String SITE_KEY = "site_id";
    public static final int INSTALL_JOB_ID = 2119;
    public static final int HELPER_INSTALL_JOB_ID = 2120;
    public static final String HELPER_PACKAGE = "com.example.retailhelper";
}
