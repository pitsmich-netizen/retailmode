package com.example.retaildpc;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * A separate short-lived encrypted credential for retrying the very same
 * Retail Mode registration without retyping the QR secret.
 *
 * This is not the service-panel PIN. No hardcoded retail password, no logs,
 * no plaintext SharedPreferences. AndroidKeyStore is required; on failure the
 * operator must enter the password again. Clear after confirmed registration.
 */
public final class RetryRetailCredential {
    private static final String PREF="retail_retry_credential_0119";
    private static final String KEY_ALIAS="retail_retry_aes_gcm_0119";
    private static final long MAX_AGE_MS=72L*60L*60L*1000L;
    private RetryRetailCredential() {}

    private static SecretKey getOrCreate() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        KeyStore.Entry entry=ks.getEntry(KEY_ALIAS,null);
        if(entry instanceof KeyStore.SecretKeyEntry)
            return ((KeyStore.SecretKeyEntry)entry).getSecretKey();
        KeyGenerator generator=KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false).build());
        return generator.generateKey();
    }

    public static synchronized boolean save(Context c, String password) {
        if(password==null||password.isEmpty()||password.length()>128)return false;
        byte[] raw=password.getBytes(StandardCharsets.UTF_8);
        try{
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,getOrCreate());
            byte[] encrypted=cipher.doFinal(raw);
            try{
                return c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit()
                    .putString("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP))
                    .putString("ct",Base64.encodeToString(encrypted,Base64.NO_WRAP))
                    .putLong("created_at",System.currentTimeMillis())
                    .commit();
            } finally {Arrays.fill(encrypted,(byte)0);}
        }catch(Exception e){
            State.log(c,"Hasło powtórzenia: Android Keystore niedostępny; wymagane wpisanie ręczne");
            return false;
        }finally{Arrays.fill(raw,(byte)0);}
    }

    public static synchronized boolean available(Context c) {
        SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        long created=p.getLong("created_at",0L);
        if(created<=0L)return false;
        if(System.currentTimeMillis()<created||
            System.currentTimeMillis()-created>MAX_AGE_MS){
            clear(c);
            return false;
        }
        return !p.getString("ct","").isEmpty()&&!p.getString("iv","").isEmpty();
    }

    public static synchronized String read(Context c) {
        if(!available(c))return null;
        SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        byte[] raw=null;
        try{
            KeyStore ks=KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            KeyStore.Entry entry=ks.getEntry(KEY_ALIAS,null);
            if(!(entry instanceof KeyStore.SecretKeyEntry))return null;
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                ((KeyStore.SecretKeyEntry)entry).getSecretKey(),
                new GCMParameterSpec(128,Base64.decode(p.getString("iv",""),Base64.NO_WRAP)));
            raw=cipher.doFinal(Base64.decode(p.getString("ct",""),Base64.NO_WRAP));
            return new String(raw,StandardCharsets.UTF_8);
        }catch(Exception e){
            clear(c);
            State.log(c,"Hasło powtórzenia: brak możliwości odszyfrowania; wpisz je ręcznie");
            return null;
        }finally{
            if(raw!=null)Arrays.fill(raw,(byte)0);
        }
    }

    public static synchronized void clear(Context c) {
        c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().clear().commit();
        try{
            KeyStore ks=KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if(ks.containsAlias(KEY_ALIAS))ks.deleteEntry(KEY_ALIAS);
        }catch(Exception ignored){}
    }
}
