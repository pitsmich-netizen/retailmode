package com.example.retaildpc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Test-only QR Retail Mode credential.
 * NEVER ship a fixed password in the APK or GitHub repository.
 * Persist a short-lived, encrypted ciphertext only until DPC handoff.
 * If Android Keystore is unavailable during Setup Wizard, keep a volatile RAM
 * fallback; if Android kills the process, require manual password entry.
 */
final class QrRetailCredential {
    private static final String P = "retail_qr_test_transient_v1";
    private static final String ALIAS = "retail_qr_handoff_key_v1";
    private static final String CIPHERTEXT = "ciphertext";
    private static final String IV = "iv";
    private QrRetailCredential() { }

    static void accept(Context c, PersistableBundle extras) {
        if (extras == null || !extras.containsKey("retail_password")) return;
        String value = extras.getString("retail_password", "");
        if (value == null || value.isEmpty() || value.length() > 128) {
            State.log(c, "QR: hasło testowe ma nieprawidłowy format");
            return;
        }
        char[] temporary = value.toCharArray();
        SecretMemory.set(temporary);
        Arrays.fill(temporary, '\0');
        try {
            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            store.load(null);
            SecretKey key;
            KeyStore.Entry old = store.getEntry(ALIAS, null);
            if (old instanceof KeyStore.SecretKeyEntry) {
                key = ((KeyStore.SecretKeyEntry) old).getSecretKey();
            } else {
                KeyGenerator generator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false)
                    .build());
                key = generator.generateKey();
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            c.getSharedPreferences(P, Context.MODE_PRIVATE).edit()
                .putString(IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .putString(CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .commit();
            State.log(c, "QR: hasło Retail Mode zabezpieczone lokalnie do przekazania Helperowi");
        } catch (Exception ex) {
            State.log(c, "QR: szyfrowanie chwilowo niedostępne; hasło tylko w pamięci RAM");
        }
    }

    static String read(Context c) {
        SharedPreferences p = c.getSharedPreferences(P, Context.MODE_PRIVATE);
        String encoded = p.getString(CIPHERTEXT, "");
        String encodedIv = p.getString(IV, "");
        if (!encoded.isEmpty() && !encodedIv.isEmpty()) {
            try {
                KeyStore store = KeyStore.getInstance("AndroidKeyStore");
                store.load(null);
                KeyStore.Entry entry = store.getEntry(ALIAS, null);
                if (entry instanceof KeyStore.SecretKeyEntry) {
                    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                    cipher.init(Cipher.DECRYPT_MODE,
                        ((KeyStore.SecretKeyEntry) entry).getSecretKey(),
                        new GCMParameterSpec(128, Base64.decode(encodedIv, Base64.NO_WRAP)));
                    byte[] plaintext = cipher.doFinal(Base64.decode(encoded, Base64.NO_WRAP));
                    String value = new String(plaintext, StandardCharsets.UTF_8);
                    Arrays.fill(plaintext, (byte) 0);
                    return value;
                }
            } catch (Exception ex) {
                State.log(c, "QR: nie udało się odszyfrować hasła; tryb ręczny pozostaje dostępny");
            }
        }
        return SecretMemory.available() ? SecretMemory.takeForInput() : null;
    }

    static void clear(Context c) {
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().clear().commit();
        SecretMemory.clear();
        try {
            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            store.load(null);
            if (store.containsAlias(ALIAS)) store.deleteEntry(ALIAS);
        } catch (Exception ignored) { }
    }
}
