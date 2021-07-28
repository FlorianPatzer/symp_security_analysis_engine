package de.fraunhofer.iosb.svs.sae.exceptions;

public class ResourceAlreadyExistsException extends RuntimeException {
    public ResourceAlreadyExistsException(String item, String field, String value) {
        super("Resource '" + item + "' with field '" + field + "' and value '" + value + "' already exists");
    }

}
