package de.fraunhofer.iosb.svs.sae;

import de.fraunhofer.iosb.svs.sae.db.Analysis;
import de.fraunhofer.iosb.svs.sae.db.Workflow;
import de.fraunhofer.iosb.svs.sae.restclient.camunda.CamundaProcessInstance;
import de.fraunhofer.iosb.svs.sae.restclient.camunda.CamundaProcessInstanceHistory;
import de.fraunhofer.iosb.svs.sae.workflowmanager.model.Phase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class AnalysisState {
    private static final Logger log = LoggerFactory.getLogger(AnalysisState.class);

    private final Analysis analysis;
    private Phase currentPhase;
    private CamundaProcessInstance startedProcessInstance;
    private Map<String, CamundaProcessInstanceHistory> completedProcessInstances;
    private CamundaProcessInstanceHistory suspendedProcessInstance;
    private Boolean finished;
    private String outputOntologyName;

    public AnalysisState(Analysis analysis) {
        this.analysis = analysis;
        this.finished = false;
        this.currentPhase = Phase.STATIC_KNOWLEDGE_EXTENSION;
        this.completedProcessInstances = new HashMap<>();
    }

    public Workflow getCurrentPhaseWorkflow() {
        return analysis.getWorkflows().stream()
                .filter(workflow -> workflow.getPhase() == currentPhase)
                .findFirst()
                .orElseThrow(() -> {
                    log.error("No workflow for Analysis {} for Phase {} found", analysis.getName(), currentPhase);
                    return new RuntimeException("No workflow for Analysis {} for Phase {} found");
                });
    }

    public Boolean isFinished() {
        return this.finished;
    }

    public Phase getCurrentPhase() {
        return currentPhase;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public CamundaProcessInstance getStartedProcessInstance() {
        return startedProcessInstance;
    }

    public void setStartedProcessInstance(CamundaProcessInstance startedProcessInstance) {
        this.startedProcessInstance = startedProcessInstance;
    }

    public CamundaProcessInstanceHistory getSuspendedProcessInstance() {
        return suspendedProcessInstance;
    }

    public void setSuspendedProcessInstance(CamundaProcessInstanceHistory suspendedProcessInstance) {
        this.suspendedProcessInstance = suspendedProcessInstance;
    }

    public Map<String, CamundaProcessInstanceHistory> getCompletedProcessInstances() {
        return completedProcessInstances;
    }

    public String getOutputOntologyName() {
        return outputOntologyName;
    }

    public void completedProcess(String processInstanceId, CamundaProcessInstanceHistory history) {
        if (startedProcessInstance.getId().equals(processInstanceId)) {
            completedProcessInstances.put(processInstanceId, history);
            // start next process
            if (currentPhase == Phase.ANALYSIS) {
                // we finished
                outputOntologyName = getCurrentPhaseWorkflow().getOutputOntologyName();
                currentPhase = null;
                startedProcessInstance = null;
                finished = true;
            } else if (currentPhase == Phase.DYNAMIC_KNOWLEDGE_EXTENSION) {
                currentPhase = Phase.ANALYSIS;
            } else if (currentPhase == Phase.STATIC_KNOWLEDGE_EXTENSION) {
                currentPhase = Phase.DYNAMIC_KNOWLEDGE_EXTENSION;
            }
        }
    }
}
