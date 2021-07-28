package de.fraunhofer.iosb.svs.sae;

import de.fraunhofer.iosb.svs.sae.db.AnalysisReport;
import de.fraunhofer.iosb.svs.sae.db.AnalysisReportRepository;
import de.fraunhofer.iosb.svs.sae.exceptions.ResourceNotFoundException;

import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private final AnalysisReportRepository analysisReportRepository;

    public ReportService(AnalysisReportRepository analysisReportRepository) {
        this.analysisReportRepository = analysisReportRepository;
    }

    public AnalysisReport getAnalysisReportById(Long id) {
        return analysisReportRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Analysis Report", id));
    }
}
