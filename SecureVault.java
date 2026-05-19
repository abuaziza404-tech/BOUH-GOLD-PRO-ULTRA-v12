package com.aboaziza.bouhterrain;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SecureVault {
    private static final String KEY_ALIAS = "aboaziza_bouh_terrain_aes256";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final int IV_SIZE = 12;
    private static final int TAG_BITS = 128;

    private final Context context;

    public SecureVault(Context context) {
        this.context = context.getApplicationContext();
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
            return entry.getSecretKey();
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
         .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
         .setKeySize(256)
         .build();
        keyGenerator.init(spec);
        return keyGenerator.generateKey();
    }

    public void saveEncrypted(String fileName, byte[] plain) throws Exception {
        SecretKey key = getOrCreateKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] iv = cipher.getIV();
        byte[] enc = cipher.doFinal(plain);

        File f = new File(context.getFilesDir(), fileName);
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(iv.length);
            out.write(iv);
            out.write(enc);
        }
    }

    public byte[] loadEncrypted(String fileName) throws Exception {
        File f = new File(context.getFilesDir(), fileName);
        if (!f.exists()) return new byte[0];
        byte[] all;
        try (FileInputStream in = new FileInputStream(f)) {
            all = new byte[(int) f.length()];
            int read = in.read(all);
            if (read != all.length) throw new IllegalStateException("Unable to read encrypted payload");
        }
        int ivLen = all[0] & 0xff;
        if (ivLen != IV_SIZE) throw new IllegalStateException("Invalid IV length");
        byte[] iv = new byte[ivLen];
        System.arraycopy(all, 1, iv, 0, ivLen);
        byte[] enc = new byte[all.length - 1 - ivLen];
        System.arraycopy(all, 1 + ivLen, enc, 0, enc.length);

        SecretKey key = getOrCreateKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        return cipher.doFinal(enc);
    }
}
