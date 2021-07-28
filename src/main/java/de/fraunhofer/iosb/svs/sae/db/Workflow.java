package de.fraunhofer.iosb.svs.sae.db;

import de.fraunhofer.iosb.svs.sae.workflowmanager.model.Phase;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.io.File;

@Entity
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // first should be string filename
    private File localFile;

    //@Enumerated(EnumType.ORDINAL) ordinal for less space but then the change of order of the enum may break existing data
    @Enumerated(EnumType.STRING)
    private Phase phase;
    /**
     * This is the id camunda assigns to the process definition (after deployment this is the id field in {@link de.fraunhofer.iosb.svs.sae.restclient.camunda.CamundaDeployedProcess}
     */
    private String camundaProcessDefinitionId;
    /**
     * This is the id we assign in the bpmn file to the process (after deployment this is the key field in {@link de.fraunhofer.iosb.svs.sae.restclient.camunda.CamundaDeployedProcess}
     */
    private String processId;

    /**
     * Only exists after running the workflow
     */
    @Column
    private String outputOntologyName;

    @ManyToOne
    @JoinColumn(name = "analysis_id")
    @JsonIgnore
    private Analysis analysis;

    public File getLocalFile() {
        return localFile;
    }

    public void setLocalFile(File localFile) {
        this.localFile = localFile;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public String getCamundaProcessDefinitionId() {
        return camundaProcessDefinitionId;
    }

    public void setCamundaProcessDefinitionId(String camundaProcessDefinitionId) {
        this.camundaProcessDefinitionId = camundaProcessDefinitionId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOutputOntologyName() {
        return outputOntologyName;
    }

    public void setOutputOntologyName(String outputOntologyName) {
        this.outputOntologyName = outputOntologyName;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }
}
