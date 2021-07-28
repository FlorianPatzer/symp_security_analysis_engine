package de.fraunhofer.iosb.svs.sae;

import de.fraunhofer.iosb.svs.sae.restclient.camunda.CamundaProcessInstanceHistory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class AsyncService {
    private static final Logger log = LoggerFactory.getLogger(AsyncService.class);

    private final MainEngineService mainEngineService;

    @Autowired
    public AsyncService(@Lazy MainEngineService mainEngineService) {
        this.mainEngineService = mainEngineService;
    }

    @Async
    public void camundaProcessMonitor(Long analysisId, String processInstanceId, long millies) throws InterruptedException {
        while (true) {
            try {
                CamundaProcessInstanceHistory history = mainEngineService.getCurrentCamundaProcessInstanceHistory(processInstanceId);
                log.debug("Status of Workflow for Analysis {} is {}", analysisId, history.getState());
                if (history.getState().equals("COMPLETED")) {
                    mainEngineService.completedProcess(analysisId, processInstanceId, history);
                    break;
                } else if (!history.getState().equals("ACTIVE")) {
                    mainEngineService.suspendedProcess(analysisId, processInstanceId, history);
                    break;
                }
            } catch (RestClientException ex) {
                log.warn("Problem connecting to Camunda: {}", ex.getMessage());
                mainEngineService.reportError(analysisId, ex);
                break;
            }
            Thread.sleep(millies);
        }
    }
}
