package de.fraunhofer.iosb.svs.sae.restclient.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/*
 * JSON mapping class representing a process instance as returned by the rest method which starts a process.
 * The mapping follows the Camunda documentation's result fields:
 * id					String	The id of the process instance.
 * processDefinitionId	String	The id of the process definition that this process instance belongs to.
 * startTime			String	The time the instance was started. Default format* yyyy-MM-dd'T'HH:mm:ss.SSSZ.
 * endTime				String	The time the instance ended. Default format* yyyy-MM-dd'T'HH:mm:ss.SSSZ.
 * durationInMillis		Number	The time the instance took to finish (in milliseconds).
 * state				String	last state of the process instance, possible values are:
 * 			ACTIVE - running process instance
 * 			SUSPENDED - suspended process instances
 * 			COMPLETED - completed through normal end event
 * 			EXTERNALLY_TERMINATED - terminated externally, for instance through REST API
 * 			INTERNALLY_TERMINATED - terminated internally, for instance by terminating boundary event
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class CamundaProcessInstanceHistory {
    // The id of the process instance.
    private String id;
    // The id of the process definition.
    private String processDefinitionId;
    // The time the instance was started. Default format* yyyy-MM-dd'T'HH:mm:ss.SSSZ.
    private String startTime;
    // The time the instance ended. Default format* yyyy-MM-dd'T'HH:mm:ss.SSSZ.
    private String endTime;
    // The time the instance took to finish (in milliseconds).
    private String durationInMillis;
    // last state of the process instance
    private String state;

    public CamundaProcessInstanceHistory() {

    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getDurationInMillis() {
        return durationInMillis;
    }

    public void setDurationInMillis(String durationInMillis) {
        this.durationInMillis = durationInMillis;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String definitionId) {
        this.processDefinitionId = definitionId;
    }


}
