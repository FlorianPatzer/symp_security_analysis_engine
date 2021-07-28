package de.fraunhofer.iosb.svs.sae.workflowmanager.model;

import org.apache.jena.ontology.Individual;

import static de.fraunhofer.iosb.svs.sae.workflowmanager.model.Policy.POLICY_BASED_ANALYSIS_NS;

public class Query extends Thing {

    public static final String QUERY_URI = POLICY_BASED_ANALYSIS_NS + "Query";
    public static final String ANALYTIC_QUERY_URI = POLICY_BASED_ANALYSIS_NS + "AnalyticQuery";

    private String query;

    public Query(Individual individual) {
        super(individual);
        //fill query
        query = getMustExistLiteral(DataProperty.QUERY_URI).getString();
    }
}
