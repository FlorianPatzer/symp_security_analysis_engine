package de.fraunhofer.iosb.svs.sae.restclient.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/*
 * JSON mapping class for a Camunda Deployed Process, following the Camunda documentation's result fields:
 * id	String	The id of the process definition.
 * key	String	The key of the process definition, i.e. the id of the BPMN 2.0 XML process definition.
 * category	String	The category of the process definition.
 * description	String	The description of the process definition.
 * name	String	The name of the process definition.
 * version	Number	The version of the process definition that the engine assigned to it.
 * resource	String	The file name of the process definition.
 * deploymentId	String	The deployment id of the process definition.
 * diagram	String	The file name of the process definition diagram, if it exists.
 * suspended	Boolean	A flag indicating whether the definition is suspended or not.*/

@JsonIgnoreProperties(ignoreUnknown = true)
public class CamundaDeployedProcess {
    private String id;
    private String key;
    private String category;
    private String description;
    private String name;
    private Integer version;
    private String resource;
    private String deploymentId;
    private String diagram;
    private String suspended;

    public CamundaDeployedProcess() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getDiagram() {
        return diagram;
    }

    public void setDiagram(String diagram) {
        this.diagram = diagram;
    }

    public String getSuspended() {
        return suspended;
    }

    public void setSuspended(String suspended) {
        this.suspended = suspended;
    }

}
