package de.fraunhofer.iosb.svs.sae.workflowmanager.exceptions;

public class InvalidOntologyException extends RuntimeException {

    public InvalidOntologyException(String message) {
        super(message);
    }

    public InvalidOntologyException(String message, Throwable cause) {
        super(message, cause);
    }
}
