package de.fraunhofer.iosb.svs.sae.workflowmanager.model;

import de.fraunhofer.iosb.svs.sae.workflowmanager.bpmn.Nodeable;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.fraunhofer.iosb.svs.sae.workflowmanager.model.ObjectProperty.HAS_TOPIC_URI;
import static de.fraunhofer.iosb.svs.sae.workflowmanager.model.ObjectProperty.IS_IN_URI;
import static de.fraunhofer.iosb.svs.sae.workflowmanager.model.Policy.POLICY_BASED_ANALYSIS_NS;

public class Implementation extends Thing implements Nodeable {

    public static final String IMPLEMENTATION_URI = POLICY_BASED_ANALYSIS_NS + "Implementation";
    public static final String LABELING_IMPLEMENTATION_URI = POLICY_BASED_ANALYSIS_NS + "LabelingImplementation";
    public static final String PROCESSING_MODULE_URI = POLICY_BASED_ANALYSIS_NS + "ProcessingModule";
    public static final String PROCESSING_RULE_URI = POLICY_BASED_ANALYSIS_NS + "ProcessingRule";
    public static final String JENA_RULE_URI = POLICY_BASED_ANALYSIS_NS + "JenaRule";
    public static final String SWRL_RULE_URI = POLICY_BASED_ANALYSIS_NS + "SwrlRule";


    private static final Logger log = LoggerFactory.getLogger(Implementation.class);
    private Phase phase;
    private String topicName;

    public Implementation(Individual individual) {
        super(individual);
        OntModel ontModel = individual.getOntModel();
        // fill phase
        Property isInProperty = ontModel.getProperty(IS_IN_URI);
        String phaseUri = individual.getPropertyValue(isInProperty).asResource().getURI();
        this.phase = Phase.getPhaseByUri(phaseUri);
        // fill topic
        Individual topic = getMustExistIndividual(HAS_TOPIC_URI);
        Literal name = getMustExistLiteral(topic, DataProperty.NAME_URI);
        this.topicName = name.getString();
    }

    public Phase getPhase() {
        return phase;
    }

    /**
     * Only local setter. The wrapped individual is not touched.
     *
     * @param phase the new phase
     */
    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public String getTopicName() {
        return topicName;
    }
}
