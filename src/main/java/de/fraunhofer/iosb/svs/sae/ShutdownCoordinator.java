package de.fraunhofer.iosb.svs.sae;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Component
public class ShutdownCoordinator {
    private static final Logger log = LoggerFactory.getLogger(ShutdownCoordinator.class);

    private final DockerService dockerService;

    public ShutdownCoordinator(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    @PreDestroy
    public void destroy() {
        log.info("Stopping started docker containers");
        dockerService.shutdownStartedContainers();

        // TODO stop camunda processes?
    }
}
