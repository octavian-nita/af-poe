package net.appfold.crypto;

import javax.validation.constraints.NotNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.Provider;
import java.security.SecureRandom;

/**
 * Largely inspired by / adapted from <a href="https://github.com/patrickfav/armadillo">Armadillo</a>'s
 * <a href="https://github.com/patrickfav/armadillo/blob/master/armadillo/src/main/java/at/favre/lib/armadillo/AuthenticatedEncryption.java">
 * AuthenticatedEncryption</a> interface. Kudos go to <a href="https://proandroiddev.com/@patrickfav">Patrick
 * Favre-Bulle</a>.
 *
 * @author Octavian Theodor NITA (https://github.com/octavian-nita/)
 * @version 1.0, 2018/04/30
 * @see <a href="https://en.wikipedia.org/wiki/Authenticated_encryption">Authenticated encryption</a> entry on Wikipedia
 * @see <a href="https://proandroiddev.com/security-best-practices-symmetric-encryption-with-aes-in-java-7616beaaade9">
 * Security Best Practices: Symmetric Encryption with AES in Java and Android</a>
 */
public interface AuthenticatedCipher {

    /**
     * @param key            encryption key
     * @param plaintext      information to encrypt
     * @param associatedData optional ({@code null}-able) authentication tag;
     *                       will be subject to integrity/authentication check
     * @return encrypted content (ciphertext)
     * @throws CryptoException if encryption fails in any way
     */
    byte[] encrypt(@NotNull byte[] key, @NotNull byte[] plaintext, byte[] associatedData) throws CryptoException;

    /**
     * Equivalent to {@link ObjectOutputStream#writeObject(Object) serializing} all {@code associatedData} items and
     * invoking {@link #encrypt(byte[], byte[], byte[]) encrypt(key, plaintext, serialized)}.
     */
    default byte[] encrypt(@NotNull byte[] key, @NotNull byte[] plaintext, Serializable... associatedData)
        throws CryptoException {
        try (final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             final ObjectOutputStream assoc = new ObjectOutputStream(bytes)) {

            assoc.writeObject(associatedData);
            assoc.flush();

            return encrypt(key, plaintext, bytes.toByteArray());

        } catch (IOException ioe) {
            throw new CryptoException("Cannot serialize associated data for encryption", ioe);
        }
    }

    /**
     * Equivalent to invoking {@link #encrypt(byte[], byte[], byte[]) encrypt(key, plaintext, (byte[]) null)}.
     */
    default byte[] encrypt(@NotNull byte[] key, @NotNull byte[] plaintext) throws CryptoException {
        return encrypt(key, plaintext, (byte[]) null);
    }

    /**
     * @param key            decryption key
     * @param ciphertext     information to decrypt
     * @param associatedData optional ({@code null}-able) authentication tag;
     *                       must be same as the one provided in the encryption step
     * @return decrypted, original information (plaintext)
     * @throws CryptoException if any of the decryption or authentication or integrity checks fail
     */
    byte[] decrypt(@NotNull byte[] key, @NotNull byte[] ciphertext, byte[] associatedData) throws CryptoException;

    /**
     * Equivalent to {@link ObjectOutputStream#writeObject(Object) serializing} all {@code associatedData} items and
     * invoking {@link #decrypt(byte[], byte[], byte[]) decrypt(key, ciphertext, serialized)}.
     */
    default byte[] decrypt(@NotNull byte[] key, @NotNull byte[] ciphertext, Serializable... associatedData)
        throws CryptoException {
        try (final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             final ObjectOutputStream assoc = new ObjectOutputStream(bytes)) {

            assoc.writeObject(associatedData);
            assoc.flush();

            return decrypt(key, ciphertext, bytes.toByteArray());

        } catch (IOException ioe) {
            throw new CryptoException("Cannot serialize associated data for decryption", ioe);
        }
    }

    /**
     * Equivalent to invoking {@link #decrypt(byte[], byte[], byte[]) decrypt(key, ciphertext, (byte[]) null)}.
     */
    default byte[] decrypt(@NotNull byte[] key, @NotNull byte[] ciphertext) throws CryptoException {
        return decrypt(key, ciphertext, (byte[]) null);
    }

    static AuthenticatedCipher cipher() { return new ArmadilloCipher(); }

    static AuthenticatedCipher cipher(Provider provider) { return new ArmadilloCipher(new SecureRandom(), provider); }
}
