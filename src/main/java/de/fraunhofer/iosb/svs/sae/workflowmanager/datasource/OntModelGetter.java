package de.fraunhofer.iosb.svs.sae.workflowmanager.datasource;

import org.apache.jena.ontology.OntModel;

public interface OntModelGetter {

    OntModel getOntModel(String name);

}
