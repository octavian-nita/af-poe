package net.appfold.crypto;

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
public class ArmadilloCipher implements AuthenticatedCipher {

    @Override
    public byte[] encrypt(byte[] key, byte[] plaintext, byte[] associatedData) throws CryptoException {
        return new byte[0];
    }

    @Override
    public byte[] decrypt(byte[] key, byte[] ciphertext, byte[] associatedData) throws CryptoException {
        return new byte[0];
    }
}
