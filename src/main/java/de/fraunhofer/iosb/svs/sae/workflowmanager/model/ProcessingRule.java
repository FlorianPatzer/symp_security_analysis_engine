package de.fraunhofer.iosb.svs.sae.workflowmanager.model;

import org.apache.jena.ontology.Individual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessingRule extends Implementation {
    private static final Logger log = LoggerFactory.getLogger(ProcessingRule.class);
    private final String rule;

    public ProcessingRule(Individual individual) {
        super(individual);
        //fill rule
        rule = getMustExistLiteral(DataProperty.RULE_URI).getString();
    }

    public String getRule() {
        return rule;
    }
}
