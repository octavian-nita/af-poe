package net.appfold.crypto;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.constraints.NotNull;
import java.nio.ByteBuffer;
import java.security.*;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.wrap;
import static java.util.Objects.requireNonNull;
import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

/**
 * Largely inspired by / adapted from <a href="https://github.com/patrickfav/armadillo">Armadillo</a>'s
 * <a href="https://github.com/patrickfav/armadillo/blob/master/armadillo/src/main/java/at/favre/lib/armadillo/AesGcmEncryption.java">
 * AesGcmEncryption</a> interface (hence the name :) ). Kudos go to <a href="https://proandroiddev.com/@patrickfav">
 * Patrick Favre-Bulle</a>.
 *
 * @author Octavian Theodor NITA (https://github.com/octavian-nita/)
 * @version 1.0, 2018/05/02
 * @see <a href="https://en.wikipedia.org/wiki/Authenticated_encryption">Authenticated encryption</a> entry on Wikipedia
 * @see <a href="https://proandroiddev.com/security-best-practices-symmetric-encryption-with-aes-in-java-7616beaaade9">
 * Security Best Practices: Symmetric Encryption with AES in Java and Android</a>
 */
final class ArmadilloCipher implements AuthenticatedCipher {

    private static final int TAG_LENGTH = 128;

    private final SecureRandom secureRandom;

    private boolean wipeKey = true;

    private final Cipher cipher;

    ArmadilloCipher() { this(new SecureRandom(), null); }

    ArmadilloCipher(@NotNull SecureRandom secureRandom) { this(secureRandom, null); }

    ArmadilloCipher(@NotNull SecureRandom secureRandom, Provider securityProvider) {
        this.secureRandom = requireNonNull(secureRandom, "If provided, the cryptographically strong " +
                                                         "random number generator (RNG) cannot be null");

        final String algorithm = "AES/GCM/NoPadding";
        try {
            this.cipher = securityProvider == null ? Cipher.getInstance(algorithm)
                                                   : Cipher.getInstance(algorithm, securityProvider);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Cannot obtain a cryptographic cipher instance", e);
        }
    }

    ArmadilloCipher setWipeKey(boolean wipeKey) {
        this.wipeKey = wipeKey;
        return this;
    }

    @Override
    public byte[] encrypt(@NotNull byte[] key, @NotNull byte[] plaintext, byte[] associatedData)
        throws CryptoException {

        requireNonNull(key, "The encryption key cannot be null");
        requireNonNull(plaintext, "The information to encrypt cannot be null");

        if (key.length < 16) {
            throw new IllegalArgumentException("The encryption key length must be longer than 16 bytes");
        }

        try {
            final byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);

            cipher.init(ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH, iv));
            if (wipeKey) {
                secureRandom.nextBytes(key);
            }
            if (associatedData != null) {
                cipher.updateAAD(associatedData);
            }
            final byte[] encrypted = cipher.doFinal(plaintext);

            return allocate(1 + iv.length + encrypted.length).put((byte) iv.length).put(iv).put(encrypted).array();

        } catch (InvalidKeyException | InvalidAlgorithmParameterException |//@fmt:off
                 IllegalBlockSizeException | BadPaddingException e) {      //@fmt:on

            throw new CryptoException("Cannot encrypt", e);
        }
    }

    @Override
    public byte[] decrypt(@NotNull byte[] key, @NotNull byte[] ciphertext, byte[] associatedData)
        throws CryptoException {

        requireNonNull(key, "The decryption key cannot be null");
        requireNonNull(ciphertext, "The information to decrypt cannot be null");

        try {
            final ByteBuffer buffer = wrap(ciphertext);

            final byte[] iv = new byte[buffer.get()];
            buffer.get(iv);

            final byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            cipher.init(DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH, iv));
            if (wipeKey) {
                secureRandom.nextBytes(key);
            }
            if (associatedData != null) {
                cipher.updateAAD(associatedData);
            }
            final byte[] plaintext = cipher.doFinal(encrypted);

            secureRandom.nextBytes(iv);
            secureRandom.nextBytes(encrypted);
            return plaintext;

        } catch (InvalidKeyException | InvalidAlgorithmParameterException |//@fmt:off
                 IllegalBlockSizeException | BadPaddingException e) {      //@fmt:on

            throw new CryptoException("Cannot decrypt", e);
        }
    }
}
