package de.fraunhofer.iosb.svs.sae.exceptions;

public class AnalysisCreationException extends RuntimeException {
    public AnalysisCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AnalysisCreationException(String message) {
        super(message);
    }
}
