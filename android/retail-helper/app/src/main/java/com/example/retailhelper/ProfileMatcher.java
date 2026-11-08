package com.example.retailhelper;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** String-only, intentionally strict matching of public marketing device names. */
final class ProfileMatcher {
    private ProfileMatcher() {}

    // Examples: S26, S10, A57, S26 Ultra, Galaxy S26.
    private static final Pattern MODEL = Pattern.compile(
        "(?i)(?<![A-Za-z0-9])(?:Galaxy\\s+)?(S\\d{1,2}(?:\\s+(?:Ultra|Plus|FE))?|A\\d{1,2}|Z\\s*(?:Fold|Flip)\\s*\\d{1,2})(?![A-Za-z0-9])");

    static String canonicalModel(String raw) {
        if (raw == null) return "";
        Matcher m = MODEL.matcher(raw);
        if (!m.find()) return "";
        String token = m.group(1).trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        // When a user entered extra text (e.g. 'Galaxy S26'), make sure
        // the model token is a unique identifiable part of that value.
        if (m.find()) return "";
        return token;
    }

    static boolean isExactExperience(String profileLabel, String selectedModel) {
        if (profileLabel == null || selectedModel == null) return false;
        String model = canonicalModel(selectedModel);
        if (model.isEmpty()) return false;
        String label = profileLabel.trim().replaceAll("\\s+", " ")
            .replaceFirst("(?i)^Galaxy\\s+", "")
            .toUpperCase(Locale.ROOT);
        return label.equals(model + " EXPERIENCE");
    }
}
