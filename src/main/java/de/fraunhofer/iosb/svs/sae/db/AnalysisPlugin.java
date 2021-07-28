package de.fraunhofer.iosb.svs.sae.db;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.List;

@Entity
public class AnalysisPlugin {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToMany(mappedBy = "analysisPlugin")
    @JsonIgnore
    private List<PolicyAnalysis> policyAnalyses;

    private String pluginFileName;

    private String pluginClasspath;

    private String resultClasspath;

    public AnalysisPlugin(Long id, String pluginFileName, String pluginClasspath, String resultClasspath) {
        this.id = id;
        this.pluginFileName = pluginFileName;
        this.pluginClasspath = pluginClasspath;
        this.resultClasspath = resultClasspath;
    }

    public AnalysisPlugin() {
        this.id = null;
        this.policyAnalyses = null;
        this.pluginFileName = null;
        this.pluginClasspath = null;
        this.resultClasspath = null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<PolicyAnalysis> getPolicyAnalyses() {
        return policyAnalyses;
    }

    public void setPolicyAnalyses(List<PolicyAnalysis> policyAnalyses) {
        this.policyAnalyses = policyAnalyses;
    }

    public String getPluginFileName() {
        return pluginFileName;
    }

    public void setPluginFileName(String pluginFileName) {
        this.pluginFileName = pluginFileName;
    }

    public String getPluginClasspath() {
        return pluginClasspath;
    }

    public void setPluginClasspath(String pluginClasspath) {
        this.pluginClasspath = pluginClasspath;
    }

    public String getResultClasspath() {
        return resultClasspath;
    }

    public void setResultClasspath(String resultClasspath) {
        this.resultClasspath = resultClasspath;
    }


}
