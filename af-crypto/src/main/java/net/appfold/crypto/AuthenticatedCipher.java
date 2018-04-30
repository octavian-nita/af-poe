package net.appfold.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * Largely inspired by / adapted from <a href="https://github.com/patrickfav/armadillo">Armadillo</a>'s
 * <a href="https://github.com/patrickfav/armadillo/blob/master/armadillo/src/main/java/at/favre/lib/armadillo/AuthenticatedEncryption.java">
 * AuthenticatedEncryption</a> interface. All kudos go to <a href="https://proandroiddev.com/@patrickfav">Patrick
 * Favre-Bulle</a>.
 *
 * @author Octavian Theodor NITA (https://github.com/octavian-nita/)
 * @version 1.0, 2018/04/30
 * @see <a href="https://proandroiddev.com/security-best-practices-symmetric-encryption-with-aes-in-java-7616beaaade9">
 * Security Best Practices: Symmetric Encryption with AES in Java and Android</a>
 */
public interface AuthenticatedCipher {

    byte[] encrypt(byte[] key, byte[] data, byte[] associatedData) throws CryptoException;

    default byte[] encrypt(byte[] key, byte[] data) throws CryptoException { return encrypt(key, data, null); }

    default byte[] encrypt(byte[] key, byte[] data, Serializable associatedData) throws CryptoException {
        try (final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             final ObjectOutputStream stream = new ObjectOutputStream(bytes)) {

            stream.writeObject(associatedData);
            stream.flush();
            return encrypt(key, data, bytes.toByteArray());

        } catch (IOException ioe) {
            throw new CryptoException("Cannot serialize associated data for encryption", ioe);
        }
    }

    default byte[] encryptWithTimestamp(byte[] key, byte[] data) throws CryptoException {
        return encrypt(key, data, ZonedDateTime.now());
    }

    byte[] decrypt(byte[] key, byte[] data, byte[] associatedData) throws CryptoException;

    default byte[] decrypt(byte[] key, byte[] data) throws CryptoException { return encrypt(key, data, null); }
}
