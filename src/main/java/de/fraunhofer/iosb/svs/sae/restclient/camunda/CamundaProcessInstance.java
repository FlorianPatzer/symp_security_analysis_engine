package de.fraunhofer.iosb.svs.sae.restclient.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/*
 * JSON mapping class representing a process instance as returned by the rest method which starts a process.
 * The mapping follows the Camunda documentation's result fields:
 * id				String	The id of the process instance.
 * definitionId		String	The id of the process definition.
 * businessKey		String	The business key of the process instance.
 * ended			Boolean	A flag indicating whether the instance is still running or not.
 * suspended		Boolean	A flag indicating whether the instance is suspended or not.
 * links			Object	A JSON array containing links to interact with the instance.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class CamundaProcessInstance {
    // The id of the process instance.
    private String id;
    // The id of the process definition.
    private String definitionId;
    // The business key of the process instance.
    private String businessKey;
    //The case instance id of the process instance.
    private String caseInstanceId;
    // A flag indicating whether the instance is still running or not.
    private Boolean ended;
    // A flag indicating whether the instance is suspended or not.
    private Boolean suspended;
    // A JSON array containing links to interact with the instance.
    private CamundaRestLink link;

    public CamundaProcessInstance() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(String definitionId) {
        this.definitionId = definitionId;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    public Boolean getEnded() {
        return ended;
    }

    public void setEnded(Boolean ended) {
        this.ended = ended;
    }

    public Boolean getSuspended() {
        return suspended;
    }

    public void setSuspended(Boolean suspended) {
        this.suspended = suspended;
    }

    public CamundaRestLink getLink() {
        return link;
    }

    public void setLink(CamundaRestLink link) {
        this.link = link;
    }


}
