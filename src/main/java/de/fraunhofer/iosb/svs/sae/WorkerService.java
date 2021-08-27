package de.fraunhofer.iosb.svs.sae;

import de.fraunhofer.iosb.svs.sae.exceptions.WorkerServiceNotAvailableException;

public interface WorkerService {

    void hasOrStartWorker(String workerImage, String workerName);
    boolean isAvailable() throws WorkerServiceNotAvailableException;
    boolean imageIsAvailableLocaly(String workerImage);
}
