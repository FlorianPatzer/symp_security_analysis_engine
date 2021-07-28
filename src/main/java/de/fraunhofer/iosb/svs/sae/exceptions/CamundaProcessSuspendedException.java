package de.fraunhofer.iosb.svs.sae.exceptions;

public class CamundaProcessSuspendedException extends RuntimeException {
    public CamundaProcessSuspendedException(String message) {
        super(message);
    }

    public CamundaProcessSuspendedException(String message, Throwable cause) {
        super(message, cause);
    }
}
