package de.fraunhofer.iosb.svs.sae.exceptions;

public class TargetSystemNotReadyException extends RuntimeException {

    public TargetSystemNotReadyException(Long id) {
        super("Target System with id " + id + " is not yet available.");
    }
}
