package de.fraunhofer.iosb.svs.sae;

/**
 * Abstraction if more than the FTP mountpoint is needed
 */
public interface MountpointHandler {
    String OWL_SUFFIX = ".owl";

    String getOntologiesMountpoint();

    String getUriFor(String filename);

    String getFilenameForOwl(String filename);
}
