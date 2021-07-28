package de.fraunhofer.iosb.svs.sae.restclient.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * JSON mapping class representing a process instance as returned by the rest method which starts a process.
 * The mapping follows the Camunda documentation's result fields:
 * links 	List 	Link to the newly created deployment with method, href and rel.
 * id 	String 	The id of the deployment.
 * name 	String 	The name of the deployment.
 * source 	String 	The source of the deployment.
 * tenantId 	String 	The tenant id of the deployment.
 * deploymentTime 	String 	The time when the deployment was created.
 * deployedProcessDefinitions 	Object 	A JSON Object containing a property for each of the process definitions,
 * which are successfully deployed with that deployment. The key is the process definition id,
 * the value is a JSON Object corresponding to the process definition, which is defined in the Process Definition resource.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentWithDefinitions {
    private String id;
    private String name;
    private String source;
    private String tenantId;
    private String deploymentTime;
    private Map<String, CamundaDeployedProcess> deployedProcessDefinitions;

    public DeploymentWithDefinitions() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDeploymentTime() {
        return deploymentTime;
    }

    public void setDeploymentTime(String deploymentTime) {
        this.deploymentTime = deploymentTime;
    }

    public Map<String, CamundaDeployedProcess> getDeployedProcessDefinitions() {
        return deployedProcessDefinitions;
    }

    public void setDeployedProcessDefinitions(Map<String, CamundaDeployedProcess> deployedProcessDefinitions) {
        this.deployedProcessDefinitions = deployedProcessDefinitions;
    }
}
