package de.koudingspawn.vault.vault.communication;

public class SecretNotAccessibleException extends Exception {

    public SecretNotAccessibleException(String message) {
        super(message);
    }

    public SecretNotAccessibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
