package com.example.retaildpc;

import android.net.Uri;

/**
 * Deliberately empty until a lawful first-party/direct APK URL and the exact
 * SHA-256 of that APK have been independently verified.
 * Do not put app-store HTML links, scraped downloads or arbitrary APK mirrors here.
 * Each source is a fixed build-time allowlist entry, not remote configuration.
 */
final class ApprovedApkCatalog {
    static final class Source {
        final String label, packageName, url, sha256;
        Source(String label, String packageName, String url, String sha256) {
            this.label=label; this.packageName=packageName;
            this.url=url; this.sha256=sha256;
        }
        boolean valid() {
            Uri u=Uri.parse(url);
            return "https".equalsIgnoreCase(u.getScheme()) &&
                u.getHost()!=null && u.getUserInfo()==null &&
                packageName.matches("[a-zA-Z0-9_.]+") &&
                sha256.matches("[a-fA-F0-9]{64}");
        }
    }

    // Populate only with verified direct vendor-distributed APKs:
    // new Source("Name", "com.vendor.app", "https://vendor.example/app.apk", "<64-char sha256>")
    private static final Source[] SOURCES = {};
    static Source[] entries() { return SOURCES.clone(); }
    private ApprovedApkCatalog() {}
}
