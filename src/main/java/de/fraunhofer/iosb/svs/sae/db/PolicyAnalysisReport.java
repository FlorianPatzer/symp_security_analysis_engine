package de.fraunhofer.iosb.svs.sae.db;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

@Entity
public class PolicyAnalysisReport {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String report;

    @ManyToOne
    @JoinColumn(name = "analysis_report_id")
    @JsonIgnore
    private AnalysisReport analysisReport;

    @ManyToOne
    @JoinColumn(name = "policy_analysis_id")
    @JsonFilter("nameOnly")
    private PolicyAnalysis policyAnalysis;

    public PolicyAnalysisReport(String report) {
        this.report = report;
    }

    public PolicyAnalysisReport() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PolicyAnalysis getPolicyAnalysis() {
        return policyAnalysis;
    }

    public void setPolicyAnalysis(PolicyAnalysis policyAnalysis) {
        this.policyAnalysis = policyAnalysis;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public AnalysisReport getAnalysisReport() {
        return analysisReport;
    }

    public void setAnalysisReport(AnalysisReport analysisReport) {
        this.analysisReport = analysisReport;
    }
}
