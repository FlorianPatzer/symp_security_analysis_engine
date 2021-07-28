package de.fraunhofer.iosb.svs.sae.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String item, String id) {
        super("No '" + item + "' with id '" + id + "' found");
    }

    public ResourceNotFoundException(String item, Long id) {
        this(item, String.valueOf(id));
    }

    public ResourceNotFoundException(String customMessage) {
        super(customMessage);
    }

}
