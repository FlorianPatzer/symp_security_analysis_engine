package de.fraunhofer.iosb.svs.sae.exceptions;

public class UnsatisfiableException extends RuntimeException {
    // TODO rename

    public UnsatisfiableException(String message) {
        super(message);
    }

    public UnsatisfiableException(Throwable cause) {
        super(cause);
    }

    public UnsatisfiableException(String message, Throwable cause) {
        super(message, cause);
    }

}
