package de.fraunhofer.iosb.svs.sae.restclient.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/*
 * JSON mapping class representing a Camunda link, usually used as interaction endpoint.
 * The mapping follows not the Camunda documentation since no documentation on links was found.
 * It is only elaborated on the basis of experience.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class CamundaRestLink {
    // HTTP method
    private String method;
    // Actual link.
    private String href;
    // Relation of the link.
    private String rel;

    public CamundaRestLink() {

    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }


}
