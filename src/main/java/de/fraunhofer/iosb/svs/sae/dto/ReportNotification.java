package de.fraunhofer.iosb.svs.sae.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReportNotification {

    @JsonProperty("analysisUuid")
    private String analysisUUID;

    @JsonProperty("reportId")
    private Long reportId;

    public ReportNotification(String analysisUUID, Long reportId) {
        this.analysisUUID = analysisUUID;
        this.reportId = reportId;
    }

    public String getAnalysisUUID() {
        return analysisUUID;
    }

    public void setAnalysisUUID(String analysisUUID) {
        this.analysisUUID = analysisUUID;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }
}
