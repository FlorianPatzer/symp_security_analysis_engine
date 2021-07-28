package de.fraunhofer.iosb.svs.sae.workflowmanager.model;

import de.fraunhofer.iosb.svs.sae.workflowmanager.exceptions.InvalidOntologyException;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntologyException;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Thing {
    private static final Logger log = LoggerFactory.getLogger(Thing.class);

    private Individual individual;

    public Thing(Individual individual) {
        this.individual = individual;
    }

    public static Literal getMustExistLiteral(Individual individual, String dataPropertyString) throws InvalidOntologyException {
        DatatypeProperty dataProperty = individual.getOntModel().getDatatypeProperty(dataPropertyString);
        if (dataProperty == null) {
            throw new InvalidOntologyException("Data Property '" + dataPropertyString + "' not found");
        }
        RDFNode node = individual.getPropertyValue(dataProperty);
        if (node != null) {
            try {
                return node.asLiteral();
            } catch (OntologyException oex) {
                log.error("Could not convert to literal ", oex);
                throw new InvalidOntologyException("Could not convert to literal", oex);
            }
        } else {
            throw new InvalidOntologyException("Individual " + individual.getLocalName() + " has no property " + dataPropertyString);
        }
    }

    /**
     * Individual should only have one property of that value.
     * @param individual
     * @param objectPropertyString
     * @return
     * @throws InvalidOntologyException
     */
    public static Individual getMustExistIndividual(Individual individual, String objectPropertyString) throws InvalidOntologyException {
        org.apache.jena.ontology.ObjectProperty objectProperty = individual.getOntModel().getObjectProperty(objectPropertyString);
        if (objectProperty == null) {
            throw new InvalidOntologyException("Object Property '" + objectPropertyString + "' not found");
        }
        RDFNode node = individual.getPropertyValue(objectProperty);
        if (node != null) {
            try {
                return node.as(Individual.class);
            } catch (OntologyException oex) {
                log.error("Could not convert to individual ", oex);
                throw new InvalidOntologyException("Could not convert to individual", oex);
            }
        } else {
            throw new InvalidOntologyException("Individual " + individual.getLocalName() + " has no property " + objectPropertyString);
        }
    }

    protected Literal getMustExistLiteral(String dataPropertyString) throws InvalidOntologyException {
        // Uses individual of this object
        return Thing.getMustExistLiteral(getIndividual(), dataPropertyString);
    }

    protected Individual getMustExistIndividual(String objectPropertyString) throws InvalidOntologyException {
        // Uses individual of this object
        return Thing.getMustExistIndividual(getIndividual(), objectPropertyString);
    }

    public Individual getIndividual() {
        return individual;
    }

    public void setIndividual(Individual individual) {
        this.individual = individual;
    }

    public boolean hasIndividual(Individual individual) {
        return this.individual.equals(individual);
    }

    public String getUri() {
        return individual.getURI();
    }

    public String getLocalName() {
        return individual.getLocalName();
    }

    @Override
    public String toString() {
        return getUri();
    }


}
