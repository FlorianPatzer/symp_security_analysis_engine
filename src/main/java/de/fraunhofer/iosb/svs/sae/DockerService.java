package de.fraunhofer.iosb.svs.sae;

import de.fraunhofer.iosb.svs.sae.exceptions.AnalysisFailedException;
import de.fraunhofer.iosb.svs.sae.exceptions.WorkerServiceNotAvailableException;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetworkSettings;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.core.DockerClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.ws.rs.ProcessingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service to handle docker containers for camunda workers
 */
@Service
public class DockerService implements WorkerService {
    private static final Logger log = LoggerFactory.getLogger(DockerService.class);
    private static final String RUNNING = "running";

    private final DockerClient dockerClient;
    private final List<String> startedContainers;
    private final String networkName;
    // only access this through getNetworkId
    private String networkId;

    public DockerService(@Value("${docker.network}") String networkName) {
        dockerClient = DockerClientBuilder.getInstance().build();
        this.startedContainers = new ArrayList<>();
        this.networkName = networkName;
    }

    /**
     * Makes sure a container for the given imageName is started.
     * <p>
     * Checks if the image exists and pulls it otherwise. Then checks if a container
     * for that image exists. Starts the container if it is not yet running or
     * creates a new one.
     * <p>
     * Does not guarantee the started container is running.
     *
     * @param imageName the name of the image a container should exist or be started
     */
    private void hasOrStartContainer(String containerImage, String containerName) {
        String imageId = getImageId(containerImage);
        if (imageId == null) {
            // image is not yet available so we pull the image and create a new container
            // with the image
            pullImage(containerImage);
            createAndStartAndConnectContainer(containerImage, containerName);
        } else {
            // image is available so we check if there are containers with this image
            List<Container> containers = getContainerForImage(imageId);
            if (containers.isEmpty()) {
                // if there is none we need to create a container with this image
                log.debug("No Container for image {} exists", containerImage);
                createAndStartAndConnectContainer(containerImage, containerName);
            } else {
                // if there is a running container we are good. If not we need to start one
                if (!hasRunningAndConnectedContainer(containers)) {
                    // take the first container start it up
                    Container container = containers.get(0);
                    // if it is not connected to the network. Connect it
                    if (!isConnected(container)) {
                        // if the container is connected to the right network name but not networkId we
                        // need to discconnect the old id
                        disconnectOnWrongNetworkId(container);
                        connectToNetwork(container.getId());
                    }
                    // start it up
                    startContainer(container.getId());
                } else {
                    log.info("Container for image {} already running", containerImage);
                }
            }
        }
    }

    private String getImageId(String imageName) {
        try {
            log.info("Check if image {} exists", imageName);
            InspectImageResponse inspectImageResponse = dockerClient.inspectImageCmd(imageName).exec();
            return inspectImageResponse.getId();
        } catch (NotFoundException nfex) {
            // Image does not exist so we need to create new container
            log.debug("Image {} does not exist yet: {}", imageName, nfex.getMessage());
            return null;
        }
    }

    private List<Container> getContainerForImage(String imageId) {
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        return containers.stream().filter(container -> {
            if (container.getImageId() == null)
                return false;
            return container.getImageId().equals(imageId);
        }).collect(Collectors.toList());
    }

    private Boolean hasRunningAndConnectedContainer(List<Container> containers) {
        // stream all containers. First check if it is running then if it is connected
        // to the right network
        return containers.stream().anyMatch(container -> {
            if (container.getState().equals(RUNNING)) {
                return isConnected(container);
            }
            return false;
        });
    }

    /**
     * Takes a container and checks if it has a network with the id from
     * {@link #getNetworkId()}.
     *
     * @param container the already fetched container to check
     * @return true if the network exists, false otherwise
     */
    private boolean isConnected(Container container) {
        ContainerNetworkSettings containerNetworkSettings = container.getNetworkSettings();
        if (containerNetworkSettings != null) {
            return containerNetworkSettings.getNetworks().entrySet().stream().anyMatch(entry -> {
                if (entry.getValue().getNetworkID() != null) {
                    return entry.getValue().getNetworkID().equals(this.getNetworkId());
                }
                return false;
            });
        }
        return false;
    }

    private String getNetworkId() {
        if (networkId == null) {
            Network network = dockerClient.inspectNetworkCmd().withNetworkId(networkName).exec();
            networkId = network.getId();
        }
        return networkId;
    }

    private void startContainer(String containerId) {
        log.info("Start container {}", containerId);
        startedContainers.add(containerId);
        dockerClient.startContainerCmd(containerId).exec();
    }

    private void disconnectOnWrongNetworkId(Container container) {
        ContainerNetworkSettings containerNetworkSettings = container.getNetworkSettings();
        if (containerNetworkSettings != null) {
            containerNetworkSettings.getNetworks().forEach((key, value) -> {
                if (value.getNetworkID() != null) {
                    if (key.equals(networkName) && !value.getNetworkID().equals(this.getNetworkId())) {
                        // network id for our network name does not correspond to current network id -->
                        // reconnect network
                        disconnectFromNetwork(container.getId());
                    }
                }
            });
        }
    }

    private void disconnectFromNetwork(String containerId) {
        log.debug("Disconnect container {} from network {}", containerId, networkName);
        dockerClient.disconnectFromNetworkCmd().withNetworkId(networkName) // both network id and name work here
                .withContainerId(containerId).exec();
    }

    private void connectToNetwork(String containerId) {
        log.debug("Connect container {} to network {}", containerId, networkName);
        dockerClient.connectToNetworkCmd().withNetworkId(networkName) // both network id and name work here
                .withContainerId(containerId).exec();
    }

    private String createContainer(String containerImage, String containerName) {
        // TODO might need more here. Needs to be able to connect to camunda
        log.info("Create container {} for image {}", containerName, containerImage);
        CreateContainerResponse createContainerResponse = dockerClient.createContainerCmd(containerImage).withName(containerName).exec();
        if (createContainerResponse.getWarnings().length > 0) {
            log.warn("{}", Arrays.toString(createContainerResponse.getWarnings()));
        }
        return createContainerResponse.getId();
    }

    private void createAndStartAndConnectContainer(String containerImage, String containerName) {
        String containerId = createContainer(containerImage, containerName);
        startContainer(containerId);
        connectToNetwork(containerId);
    }

    private void pullImage(String imageName) {
        try {
            log.info("Pulling image '{}'. This might take up to two minutes", imageName);
            dockerClient.pullImageCmd(imageName).start().awaitCompletion(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            // couldn't pull image in 90 seconds
            log.error("Could not pull image {} for two minutes, aborting", imageName);
            throw new AnalysisFailedException("Could not pull Docker image " + imageName);
        }
    }

    public void shutdownStartedContainers() {
        startedContainers.forEach(this::stopContainer);
        startedContainers.clear();
    }

    private void stopContainer(String containerId) {
        log.debug("Stopping container {}", containerId);
        dockerClient.stopContainerCmd(containerId).exec();
    }

    @Override
    public void hasOrStartWorker(String workerImage, String workerName) {
        // this service uses docker containers
        hasOrStartContainer(workerImage, workerName);
    }

    @Override
    public boolean isAvailable() {
        try {
            dockerClient.pingCmd().exec();
        } catch (ProcessingException pex) {
            throw new WorkerServiceNotAvailableException("Docker Client connection problem", pex);
        }
        return true;
    }
}
