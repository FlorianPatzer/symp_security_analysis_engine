package de.fraunhofer.iosb.svs.sae.restclient.camunda;

import de.fraunhofer.iosb.svs.sae.db.Workflow;
import de.fraunhofer.iosb.svs.sae.exceptions.CamundaClientException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CamundaClient {
    private static final Logger log = LoggerFactory.getLogger(CamundaClient.class);

    private final String PROCESS_DEFINITION = "process-definition";
    private final String PROCESS_INSTANCE_HISTORY = "history/process-instance";

    private final RestTemplate restTemplate;
    private final WebClient webClient;
    private final String camundaEndpoint;

    @Autowired
    public CamundaClient(@Value("${camunda.rest-endpoint}") String camundaEndpoint,
                         RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        log.info("Using Camunda endpoint at {}", camundaEndpoint);
        this.camundaEndpoint = camundaEndpoint;
        this.webClient = WebClient.builder()
                .baseUrl(camundaEndpoint)
                .build();

    }

    /**
     * Deploy the workflows under the given name to Camunda.
     * <p>
     * Does a blocking post to Camunda with the workflows in the body.
     *
     * @param workflows      the workflows to be deployed
     * @param deploymentName the name the deployment should have
     * @return a map containing the workflow with their Camunda Process Definitions Id extracted from the Camunda response
     */
    public Map<Workflow, String> deployWorkflows(List<Workflow> workflows, String deploymentName) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("deployment-name", deploymentName);
        workflows.forEach(workflow -> bodyBuilder.part(workflow.getPhase().getLocalName(), new FileSystemResource(workflow.getLocalFile())));
        try {
            log.info("Post deployment {} to Camunda, blocking", deploymentName);
            DeploymentWithDefinitions deploymentWithDefinitions = webClient.post()
                    .uri("/deployment/create")
                    .bodyValue(bodyBuilder.build())
                    .retrieve()
                    .bodyToMono(DeploymentWithDefinitions.class)
                    .block();
            log.debug("Finished post deployment {} to Camunda", deploymentName);


            // response contains deployed process which relate our set processId in the bpmn file with the new camundaProcessId set by camunda
            // map the workflow to their Camunda Process Definition Id given from Camunda
            Map<Workflow, String> workflowDefinitionIdMap = new HashMap<>();
            if (deploymentWithDefinitions != null) {
                deploymentWithDefinitions.getDeployedProcessDefinitions().forEach((key, deployedProcess) -> {
                    log.debug("Found camundaProcessId {}", deployedProcess.getId());
                    workflows.forEach(workflow -> {
                        if (workflow.getProcessId().equals(deployedProcess.getKey())) {
                            log.debug("Found workflow with processId {} for camundaProcessId {}", workflow.getProcessId(), deployedProcess.getId());
                            workflowDefinitionIdMap.put(workflow, deployedProcess.getId());
                        }
                    });
                });
            } else {
                throw new CamundaClientException("DeploymentWithDefinitions is null");
            }
            return workflowDefinitionIdMap;
        } catch (WebClientResponseException wcrex) {
            log.error("Got a bad response from Camunda, Status Code: {} Status Text: {}", wcrex.getStatusCode(), wcrex.getStatusText());
            throw new CamundaClientException("Bad response from Camunda", wcrex);
        }
    }

    public CamundaDeployedProcess getCamundaProcessDefinition(String camundaProcessId) throws RestClientException {
        log.info("Trying to get camunda process definition for id " + camundaProcessId + ".");
        CamundaDeployedProcess[] res = restTemplate.getForObject(camundaEndpoint + "/" + PROCESS_DEFINITION, CamundaDeployedProcess[].class);
        if (res.length > 0) return res[0];
        return null;
    }

    public CamundaProcessInstance startCamundaProcess(String processId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<String>("{}", headers);
        log.info("Trying to start camunda process " + processId + ".");
        return restTemplate.postForObject(camundaEndpoint + "/" + PROCESS_DEFINITION + "/" + processId + "/start", request, CamundaProcessInstance.class);
    }

    private synchronized CamundaProcessInstanceHistory requestCamundaProcessInstanceHistory(String camundaProcessInstanceId) throws RestClientException {
        log.debug("Trying to get history for camunda process instance " + camundaProcessInstanceId + ".");
        return restTemplate.getForObject(camundaEndpoint + "/" + PROCESS_INSTANCE_HISTORY + "/" + camundaProcessInstanceId, CamundaProcessInstanceHistory.class);
    }

    public synchronized CamundaProcessInstanceHistory getCamundaProcessInstanceHistory(String processInstanceId) {
        CamundaProcessInstanceHistory history = requestCamundaProcessInstanceHistory(processInstanceId);
        log.debug("Received status '" + history.getState() + "' for process instance " + processInstanceId + ".");
        return history;
    }
}
