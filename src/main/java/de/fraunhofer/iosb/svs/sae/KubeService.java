package de.fraunhofer.iosb.svs.sae;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import de.fraunhofer.iosb.svs.sae.exceptions.WorkerServiceNotAvailableException;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

@Service
public class KubeService implements WorkerService {
    private static final Logger log = LoggerFactory.getLogger(KubeService.class);
    private final KubernetesClient kubeClient;
    private String kubeNamespace;

    public KubeService(@Value("${rancher.master.url}") String masterUrl, @Value("${rancher.token}") String token, @Value("${rancher.namespace}") String kubeNamespace) {
        this.kubeNamespace = kubeNamespace;

        ConfigBuilder configBuilder = new ConfigBuilder();
        configBuilder.withOauthToken(token);
        configBuilder.withNamespace(kubeNamespace);
        configBuilder.withMasterUrl(masterUrl);

        this.kubeClient = new DefaultKubernetesClient(configBuilder.build());
    }
    
    private Deployment findDeploymentWithtImage(String imageName) {
        DeploymentList deploymentsList = kubeClient.apps().deployments().inNamespace(kubeNamespace).list();

        for (Deployment deployment : deploymentsList.getItems()) {
            List<Container> containers = deployment.getSpec().getTemplate().getSpec().getContainers();
            for (Container container : containers) {
                if (container.getImage().equals(imageName)) {
                    return deployment;
                }
            }
        }
        return null;
    }

    private void hasOrCreateDeployment(String imageName, String containerName) {

        Deployment deployment = findDeploymentWithtImage(imageName);

        if (deployment == null) {
            createAndStartDeployment(imageName, containerName);
            log.info("Deployment with image {} created", imageName);
        } else {
            Integer availableReplicas = deployment.getStatus().getReplicas();

            if (availableReplicas.intValue() == 0) {
                log.info("No replicas with image {} found. Scaling to 1.", imageName);
                scaleDeployment(deployment, 1);
            } else {
                log.info("One replica of the deployment with image {} already running", imageName);
            }
        }
    }

    private void scaleDeployment(Deployment deployment, int replicas) {
        String deploymentName = deployment.getMetadata().getName();
        kubeClient.apps().deployments().inNamespace(kubeNamespace).withName(deploymentName).scale(replicas);
    }

    private void createAndStartDeployment(String imageName, String containerName) {
        Deployment deployment = new Deployment();
        DeploymentSpec deploymentSpec = new DeploymentSpec();
        PodTemplateSpec podTemplateSpec = new PodTemplateSpec();
        PodSpec podSpec = new PodSpec();
        List<Container> containers = new ArrayList<Container>();
        ObjectMeta deploymentMetadata = new ObjectMeta();
        ObjectMeta templateMetadata = new ObjectMeta();
        LabelSelector selector = new LabelSelector();

        Map<String, String> matchLabels = new HashMap<String, String>();
        matchLabels.put(containerName, containerName);

        // Create container
        Container container = new Container();
        container.setImage(imageName);
        
        // TODO: This is done so that minikube uses localy built iages
        // TODO: Check if it's working in the actual k8s cluster
        container.setImagePullPolicy("IfNotPresent");
        
        container.setName(containerName);
        containers.add(container);

        // Create template spec
        podSpec.setContainers(containers);
        podTemplateSpec.setSpec(podSpec);

        // Create template metadata
        templateMetadata.setLabels(matchLabels);
        podTemplateSpec.setMetadata(templateMetadata);

        // Create selector
        selector.setMatchLabels(matchLabels);

        // Create deployment spec
        deploymentSpec.setSelector(selector);
        deploymentSpec.setTemplate(podTemplateSpec);

        // Create deployment metadata
        deploymentMetadata.setName(containerName);

        // Add deployment spec and deployment metadata to the deployment
        deployment.setSpec(deploymentSpec);
        deployment.setMetadata(deploymentMetadata);

        // Create deployment
        kubeClient.apps().deployments().inNamespace(kubeNamespace).create(deployment);
        
        // Crete a number of replicas
        scaleDeployment(deployment, 1);
    }

    @Override
    public void hasOrStartWorker(String workerImage, String workerName) {
        hasOrCreateDeployment(workerImage, workerName);
    }

    @Override
    public boolean isAvailable() throws WorkerServiceNotAvailableException {
        try {
            kubeClient.apps().getApiVersion();
        } catch (ProcessingException pex) {
            throw new WorkerServiceNotAvailableException("Kubernetis Client connection problem", pex);
        }

        return true;
    }
    
    @Override
    public boolean imageIsAvailableLocaly(String workerImage) {
        // TODO: Implement
        // This now however should work for now, because we are connecting to Fraunhofer's Rancher cluster, which shouldn't have our images localy.
        return false;
    }
}
