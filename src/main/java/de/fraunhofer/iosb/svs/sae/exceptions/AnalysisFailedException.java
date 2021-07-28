package de.fraunhofer.iosb.svs.sae.exceptions;

public class AnalysisFailedException extends RuntimeException {

    private final String answerMessage;

    public AnalysisFailedException(String message, Throwable throwable) {
        super(message, throwable);
        this.answerMessage = message + " - because of - " + throwable.getMessage();
    }

    public AnalysisFailedException(String message) {
        super(message);
        this.answerMessage = message;
    }

    public String getAnswerMessage() {
        return answerMessage;
    }
}
