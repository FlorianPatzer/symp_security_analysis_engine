package de.fraunhofer.iosb.svs.sae.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReportNotification {

    @JsonProperty("reportId")
    private Long reportId;
    
    @JsonProperty("analysisId")
    private Long analysisId;

    public ReportNotification(Long analysisId, Long reportId) {
        this.analysisId = analysisId;
        this.reportId = reportId;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }
}
