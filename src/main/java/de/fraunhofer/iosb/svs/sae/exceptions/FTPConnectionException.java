package de.fraunhofer.iosb.svs.sae.exceptions;

public class FTPConnectionException extends RuntimeException {
    public FTPConnectionException(String message) {
        super(message);
    }

    public FTPConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
