package de.fraunhofer.iosb.svs.sae.restclient.sme;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TargetSystem {

    private Long id;
    private String name;

    private Set<Map<String, String>> tasks;
    private Set<Map<String, String>> ontologyDependencies;
    /**
     * If ontologyPath is null --> not yet ready
     */
    private String ontologyPath;

    /*
    TODO add here and at spc a last changed field --> make target system at analysis persistent --> before execution check if lastchanged changed at spc --> if not maybe we dont need to do analysis (depends on policyanalyses)
     */

    public TargetSystem() {
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

    public Set<Map<String, String>> getTasks() {
        return tasks;
    }

    public void setTasks(Set<Map<String, String>> tasks) {
        this.tasks = tasks;
    }

    public Set<String> getTaskIds() {
        return tasks.stream().map(map -> map.get("id")).collect(Collectors.toSet());
    }

    public Set<Map<String, String>> getOntologyDependencies() {
        return ontologyDependencies;
    }

    public void setOntologyDependencies(Set<Map<String, String>> ontologyDependencies) {
        this.ontologyDependencies = ontologyDependencies;
    }

    public Set<String> getOntologyDependencyIds() {
        return ontologyDependencies.stream().map(map -> map.get("id")).collect(Collectors.toSet());
    }

    public String getOntologyPath() {
        return ontologyPath;
    }

    public void setOntologyPath(String ontologyPath) {
        this.ontologyPath = ontologyPath;
    }

    @Override
    public String toString() {
        return "TargetSystem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", tasks=" + tasks +
                ", ontologyDependencies=" + ontologyDependencies +
                ", ontologyPath='" + ontologyPath + '\'' +
                '}';
    }
}
