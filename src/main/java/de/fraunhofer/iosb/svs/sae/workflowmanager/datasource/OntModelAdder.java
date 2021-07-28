package de.fraunhofer.iosb.svs.sae.workflowmanager.datasource;

import org.apache.jena.rdf.model.Model;

public interface OntModelAdder {
    void addModel(String name, Model ontModel);
}
