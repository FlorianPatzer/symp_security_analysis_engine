package de.fraunhofer.iosb.svs.sae.db;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.fraunhofer.iosb.svs.sae.restclient.sme.TargetSystem;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Entity
public class Analysis {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    private Long id;

    @Column(nullable = false, unique = true)
    private String uuid;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, length=1500)
    private String description;
   
    @Column(name = "owner_app_id", nullable = false)
    @JsonIgnore
    private String ownerApp;
    
    // these are only valid if the analysis is running
    @OneToOne
    private AnalysisReport currentAnalysisReport;

    @OneToMany(mappedBy = "analysis")
    private List<Workflow> workflows;

    @JsonIgnore
    private Boolean error = false;

    @Column(name = "last_invocation")
    private Timestamp lastInvocation;
    @Column(name = "last_finish")
    private Timestamp lastFinish;

    @Column(nullable = false)
    @JsonProperty("targetSystem")
    private Long targetSystemId;

    @OneToMany(mappedBy = "analysis")
    @JsonFilter("idOnly")
    private Set<AnalysisReport> analysisReports;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.PERSIST)
    @JsonFilter("shortPolicyAnalysis")
    private Set<PolicyAnalysis> policyAnalyses;

    @Transient
    @JsonIgnore
    private TargetSystem targetSystem;

    public Analysis(Long id, String name, String description, String ownerApp, Boolean error, Timestamp lastInvocation, Timestamp lastFinish, Long targetSystemId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerApp = ownerApp;
        this.error = error;
        this.lastInvocation = lastInvocation;
        this.lastFinish = lastFinish;
        this.targetSystemId = targetSystemId;
    }

    public Analysis(String uuid, String name, String description, String ownerApp, Long targetSystemId, Set<PolicyAnalysis> policyAnalyses) {
        this.uuid = uuid;
        this.name = name;
        this.description = description;
        this.ownerApp = ownerApp;
        this.targetSystemId = targetSystemId;
        this.policyAnalyses = policyAnalyses;
        policyAnalyses.forEach(policyAnalysis -> policyAnalysis.setAnalysis(this));
    }

    public Analysis() {
        this.id = null;
        this.name = null;
        this.description = null;
        this.ownerApp = null;
        this.lastInvocation = null;
        this.lastFinish = null;
        this.targetSystemId = null;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwnerAppId() {
        return ownerApp;
    }

    public void setOwnerAppId(String ownerApp) {
        this.ownerApp = ownerApp;
    }

    public Timestamp getLastInvocation() {
        return lastInvocation;
    }

    public void setLastInvocation(Timestamp lastInvocation) {
        this.lastInvocation = lastInvocation;
    }

    public Timestamp getLastFinish() {
        return lastFinish;
    }

    public void setLastFinish(Timestamp lastFinish) {
        this.lastFinish = lastFinish;
    }

    public AnalysisReport getCurrentAnalysisReport() {
        return currentAnalysisReport;
    }

    public void setCurrentAnalysisReport(AnalysisReport currentAnalysisReport) {
        this.currentAnalysisReport = currentAnalysisReport;
    }


    public Boolean getError() {
        return error;
    }

    public void setError(Boolean error) {
        this.error = error;
    }

    public Set<AnalysisReport> getAnalysisReports() {
        return analysisReports;
    }

    public void setAnalysisReports(Set<AnalysisReport> analysisReports) {
        this.analysisReports = analysisReports;
    }

    public void addAnalysisReport(AnalysisReport analysisReport) {
        if (this.analysisReports == null) {
            analysisReports = new HashSet<>();
        }
        this.analysisReports.add(analysisReport);
        analysisReport.setAnalysis(this);
    }

    public Set<PolicyAnalysis> getPolicyAnalyses() {
        return policyAnalyses;
    }

    public void setPolicyAnalyses(Set<PolicyAnalysis> policyAnalyses) {
        this.policyAnalyses = policyAnalyses;
        policyAnalyses.forEach(policyAnalysis -> policyAnalysis.setAnalysis(this));
    }

    public void addPolicyAnalysis(PolicyAnalysis policyAnalysis) {
        if (this.policyAnalyses != null) {
            policyAnalysis.setAnalysis(this);
            this.policyAnalyses.add(policyAnalysis);
        }
    }

    public Long getTargetSystemId() {
        return targetSystemId;
    }

    public void setTargetSystemId(Long targetSystemId) {
        this.targetSystemId = targetSystemId;
    }

    /**
     * Only transient, so might return null if it wasn't added
     *
     * @return
     */
    public TargetSystem getTargetSystem() {
        return targetSystem;
    }

    public void setTargetSystem(TargetSystem targetSystem) {
        this.targetSystem = targetSystem;
    }

    public List<Workflow> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(List<Workflow> workflows) {
        this.workflows = workflows;
    }

    public void addWorkflow(Workflow workflow) {
        if (this.workflows == null) {
            workflows = new ArrayList<>();
        }
        workflow.setAnalysis(this);
        workflows.add(workflow);
    }
}
