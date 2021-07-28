package de.fraunhofer.iosb.svs.sae.db;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class PolicyAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String link;

    @ManyToOne
    @JoinColumn(name = "analysis_id", updatable = true)
    @JsonIgnore
    private Analysis analysis;

    @ManyToOne(cascade = CascadeType.PERSIST, optional = true)
    @JoinColumn(name = "analysis_plugin_id", updatable = true)
    private AnalysisPlugin analysisPlugin;

    @Column(columnDefinition = "TEXT")
    private String query;

    // TODO
    private String graphName;

    @OneToMany(mappedBy = "policyAnalysis", fetch = FetchType.EAGER)
    private Set<PolicyAnalysisReport> policyAnalysisReportSet;

    public PolicyAnalysis(String name, String description, String link) {
        this.name = name;
        this.description = description;
        this.link = link;
    }

    public PolicyAnalysis() {

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

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }

    public AnalysisPlugin getAnalysisPlugin() {
        return analysisPlugin;
    }

    public void setAnalysisPlugin(AnalysisPlugin analysisPlugin) {
        this.analysisPlugin = analysisPlugin;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getGraphName() {
        return graphName;
    }

    public void setGraphName(String graphName) {
        this.graphName = graphName;
    }

    public Set<PolicyAnalysisReport> getPolicyAnalysisReportSet() {
        return policyAnalysisReportSet;
    }

    public void setPolicyAnalysisReportSet(Set<PolicyAnalysisReport> policyAnalysisReportSet) {
        this.policyAnalysisReportSet = policyAnalysisReportSet;
    }

    public void addPolicyAnalysisReport(PolicyAnalysisReport policyAnalysisReport) {
        if (this.policyAnalysisReportSet == null) {
            this.policyAnalysisReportSet = new HashSet<>();
        }
        this.policyAnalysisReportSet.add(policyAnalysisReport);
        policyAnalysisReport.setPolicyAnalysis(this);
    }
}
