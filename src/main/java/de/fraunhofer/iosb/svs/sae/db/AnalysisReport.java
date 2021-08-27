package de.fraunhofer.iosb.svs.sae.db;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonFilter;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
public class AnalysisReport {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(name = "start_time")
    private Timestamp startTime;
    @Column(name = "finish_time")
    private Timestamp finishTime;
    @Column(name = "analysis_id")
    private Long analysisId;
   
    @Column(name = "error")
    private String error;

    @OneToMany(mappedBy = "analysisReport")
    private Set<PolicyAnalysisReport> policyAnalysisReportSet;

    public AnalysisReport(Timestamp startTime, Long analysisId) {
        this.startTime = startTime;
        this.analysisId = analysisId;
        this.policyAnalysisReportSet = new HashSet<>();
    }

    public AnalysisReport() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Timestamp finishTime) {
        this.finishTime = finishTime;
    }

    public Set<PolicyAnalysisReport> getPolicyAnalysisReportSet() {
        return policyAnalysisReportSet;
    }

    public void setPolicyAnalysisReportSet(Set<PolicyAnalysisReport> policyAnalysisReportSet) {
        this.policyAnalysisReportSet = policyAnalysisReportSet;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
