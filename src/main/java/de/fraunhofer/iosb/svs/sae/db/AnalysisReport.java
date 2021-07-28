package de.fraunhofer.iosb.svs.sae.db;

import com.fasterxml.jackson.annotation.JsonFilter;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
public class AnalysisReport {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "analysis_id")
    @JsonFilter("uuidOnly")
    private Analysis analysis;
    @Column(name = "start_time")
    private Timestamp startTime;
    @Column(name = "finish_time")
    private Timestamp finishTime;

    @Column(name = "error")
    private String error;

    @OneToMany(mappedBy = "analysisReport")
    private Set<PolicyAnalysisReport> policyAnalysisReportSet;

    public AnalysisReport(Timestamp startTime) {
        this.startTime = startTime;
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

    public Analysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
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
