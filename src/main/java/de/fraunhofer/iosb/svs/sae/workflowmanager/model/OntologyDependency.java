package de.fraunhofer.iosb.svs.sae.workflowmanager.model;

import de.fraunhofer.iosb.svs.sae.workflowmanager.bpmn.Nodeable;

import org.apache.jena.ontology.Individual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.fraunhofer.iosb.svs.sae.workflowmanager.model.Policy.POLICY_BASED_ANALYSIS_NS;

public class OntologyDependency extends Thing implements Nodeable {
    public static final String ONTOLOGY_DEPENDENCY_URI = POLICY_BASED_ANALYSIS_NS + "OntologyDependency";
    public static final String ONTMAPPING_TOPIC = "ontmapping";
    private static final Logger log = LoggerFactory.getLogger(OntologyDependency.class);
    private String namespaceUri;
    private String prefix;

    public OntologyDependency(Individual individual) {
        super(individual);
        //fill uri and prefix
        namespaceUri = getMustExistLiteral(DataProperty.URI_URI).getString();
        prefix = getMustExistLiteral(DataProperty.PREFIX_URI).getString();
    }

    public String getNamespaceUri() {
        return namespaceUri;
    }

    public String getPrefix() {
        return prefix;
    }
}
