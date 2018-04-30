package net.appfold.crypto;

/**
 * Base class for checked exceptions thrown by the crypto packages.
 *
 * @author Octavian Theodor NITA (https://github.com/octavian-nita/)
 * @version 1.0, 2018/04/30
 */
public class CryptoException extends Exception {

    public CryptoException() {}

    public CryptoException(Throwable cause) { super(cause); }

    public CryptoException(String message) { super(message); }

    public CryptoException(String message, Throwable cause) { super(message, cause); }
}
