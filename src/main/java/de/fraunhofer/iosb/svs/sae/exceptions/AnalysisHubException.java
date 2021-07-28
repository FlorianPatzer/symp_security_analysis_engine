package de.fraunhofer.iosb.svs.sae.exceptions;

public class AnalysisHubException extends RuntimeException {
    public AnalysisHubException(String message) {
        super(message);
    }

    public AnalysisHubException(String message, Throwable cause) {
        super(message, cause);
    }
}
