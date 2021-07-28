package de.fraunhofer.iosb.svs.sae.workflowmanager.model;


import org.apache.jena.ontology.Individual;

public class Policy extends Thing {

    public static final String POLICY_BASED_ANALYSIS_URI = "https://iosb.fraunhofer.de/ICS-Security/policy-based-analysis";
    public static final String POLICY_BASED_ANALYSIS_URI_VERSION = POLICY_BASED_ANALYSIS_URI + "/0.1.0";
    public static final String POLICY_BASED_ANALYSIS_NS = POLICY_BASED_ANALYSIS_URI + "#";

    public Policy(Individual individual) {
        super(individual);
    }
}
