package net.nemerosa.ontrack.service.security;

import net.nemerosa.ontrack.common.Utils;
import net.nemerosa.ontrack.model.security.AbstractConfidentialStore;
import net.nemerosa.ontrack.model.support.EnvService;
import net.nemerosa.ontrack.model.support.OntrackConfigProperties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Storing the keys as files in a directory.
 */
@Component
@ConditionalOnProperty(name = OntrackConfigProperties.KEY_STORE, havingValue = "file", matchIfMissing = true)
public class FileConfidentialStore extends AbstractConfidentialStore {

    private static final String ENCODING = "UTF-8";

    /**
     * Directory that stores individual keys.
     */
    private final File rootDir;

    /**
     * The master key.
     * <p>
     * The sole purpose of the master key is to encrypt individual keys on the disk.
     * Because leaking this master key compromises all the individual keys, we must not let
     * this master key used for any other purpose, hence the protected access.
     */
    private final SecretKey masterKey;

    @Autowired
    public FileConfidentialStore(EnvService envService) throws IOException {
        this(envService.getWorkingDir("security", "secrets"));
    }

    public FileConfidentialStore(File rootDir) throws IOException {
        this.rootDir = rootDir;
        LoggerFactory.getLogger(FileConfidentialStore.class).info(
                "[key-store] Using file based key store at {}",
                rootDir.getAbsolutePath()
        );

        File masterSecret = new File(rootDir, "master.key");
        if (!masterSecret.exists()) {
            // we are only going to use small number of bits (since export control limits AES key length)
            // but let's generate a long enough key anyway
            FileUtils.write(masterSecret, Utils.toHexString(randomBytes(128)), ENCODING);
        }
        this.masterKey = SecurityUtils.toAes128Key(FileUtils.readFileToString(masterSecret, ENCODING));
    }

    /**
     * Persists the payload of a key to the disk.
     */
    @Override
    public void store(String key, byte[] payload) throws IOException {
        try {
            Cipher sym = Cipher.getInstance("AES");
            sym.init(Cipher.ENCRYPT_MODE, masterKey);
            try (
                    FileOutputStream fos = new FileOutputStream(getFileFor(key));
                    CipherOutputStream cos = new CipherOutputStream(fos, sym)
            ) {
                cos.write(payload);
                cos.write(MAGIC);
            }
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to persist the key: " + key, e);
        }
    }

    /**
     * Reverse operation of {@link #store(String, byte[])}
     *
     * @return null the data has not been previously persisted.
     */
    @Override
    public byte[] load(String key) throws IOException {
        try {
            File f = getFileFor(key);
            if (!f.exists()) return null;

            Cipher sym = Cipher.getInstance("AES");
            sym.init(Cipher.DECRYPT_MODE, masterKey);
            try (
                    FileInputStream fis = new FileInputStream(f);
                    CipherInputStream cis = new CipherInputStream(fis, sym)
            ) {
                byte[] bytes = IOUtils.toByteArray(cis);
                return verifyMagic(bytes);
            }
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to persist the key: " + key, e);
        }
    }

    /**
     * Verifies that the given byte[] has the MAGIC trailer, to verify the integrity of the decryption process.
     */
    private byte[] verifyMagic(byte[] payload) {
        int payloadLen = payload.length - MAGIC.length;
        if (payloadLen < 0) return null;    // obviously broken

        for (int i = 0; i < MAGIC.length; i++) {
            if (payload[payloadLen + i] != MAGIC[i])
                return null;    // broken
        }
        byte[] truncated = new byte[payloadLen];
        System.arraycopy(payload, 0, truncated, 0, truncated.length);
        return truncated;
    }

    private File getFileFor(String key) {
        return new File(rootDir, key);
    }

    private static final byte[] MAGIC = "::::MAGIC::::".getBytes();
}
