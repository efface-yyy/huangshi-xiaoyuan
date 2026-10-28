package com.smart.hgnu;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Device-local, authenticated encryption for one session; never stores a login password. */
final class SessionStore {
    private static final String KEY_ALIAS = "smart_hgnu_saved_session_v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final byte[] FORMAT = {'H', 'G', 'N', 'U', 1};
    private static final int IV_LENGTH = 12;
    private static final int MAX_FILE_BYTES = 128 * 1024;
    private final AtomicFile file;
    private int revision;

    SessionStore(Context context) {
        // noBackupFilesDir avoids restoring a ciphertext onto a device without its Keystore key.
        file = new AtomicFile(new File(context.getNoBackupFilesDir(), "session.enc"));
    }

    synchronized int revision() {
        return revision;
    }

    synchronized JSONObject load() throws Exception {
        if (!file.getBaseFile().exists()) return null;
        if (file.getBaseFile().length() > MAX_FILE_BYTES)
            throw new IllegalStateException("Invalid session size");
        byte[] bytes = file.readFully();
        if (bytes.length < FORMAT.length + IV_LENGTH + 16)
            throw new IllegalStateException("Invalid session format");
        ByteBuffer input = ByteBuffer.wrap(bytes);
        for (byte expected : FORMAT) {
            if (input.get() != expected) throw new IllegalStateException("Unknown session version");
        }
        byte[] iv = new byte[IV_LENGTH];
        input.get(iv);
        byte[] ciphertext = new byte[input.remaining()];
        input.get(ciphertext);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey(false), new GCMParameterSpec(128, iv));
        cipher.updateAAD(FORMAT);
        byte[] plaintext = cipher.doFinal(ciphertext);
        try {
            return new JSONObject(new String(plaintext, StandardCharsets.UTF_8));
        } finally {
            java.util.Arrays.fill(plaintext, (byte) 0);
        }
    }

    synchronized void save(JSONObject session, int expectedRevision) throws Exception {
        // A request that completes after logout must not recreate the deleted saved session.
        if (revision != expectedRevision) return;
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(true));
        cipher.updateAAD(FORMAT);
        byte[] plaintext = session.toString().getBytes(StandardCharsets.UTF_8);
        byte[] ciphertext;
        try {
            ciphertext = cipher.doFinal(plaintext);
        } finally {
            java.util.Arrays.fill(plaintext, (byte) 0);
        }
        byte[] iv = cipher.getIV();
        if (iv.length != IV_LENGTH) throw new IllegalStateException("Unexpected IV size");
        FileOutputStream output = null;
        try {
            output = file.startWrite();
            output.write(FORMAT);
            output.write(iv);
            output.write(ciphertext);
            file.finishWrite(output);
        } catch (Exception exception) {
            if (output != null) file.failWrite(output);
            throw exception;
        }
    }

    synchronized void clear() {
        revision++;
        file.delete();
        try {
            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            store.load(null);
            store.deleteEntry(KEY_ALIAS);
        } catch (Exception ignored) {
            // Removing the ciphertext is sufficient for logout; a later save can reuse the key.
        }
    }

    private SecretKey encryptionKey(boolean createIfMissing) throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(KEY_ALIAS)) return (SecretKey) store.getKey(KEY_ALIAS, null);
        if (!createIfMissing) throw new IllegalStateException("Session key no longer available");
        KeyGenerator generator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(
                new KeyGenParameterSpec.Builder(
                                KEY_ALIAS,
                                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build());
        return generator.generateKey();
    }
}
