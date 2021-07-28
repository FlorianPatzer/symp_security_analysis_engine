package de.fraunhofer.iosb.svs.sae.exceptions;

public class CamundaClientException extends RuntimeException {
    public CamundaClientException(String message) {
        super(message);
    }

    public CamundaClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
