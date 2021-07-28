package de.fraunhofer.iosb.svs.sae.workflowmanager.operations;

import de.fraunhofer.iosb.svs.sae.workflowmanager.model.Implementation;
import de.fraunhofer.iosb.svs.sae.workflowmanager.model.OntologyDependency;
import de.fraunhofer.iosb.svs.sae.workflowmanager.model.Phase;

import org.apache.jena.ontology.OntClass;

import java.util.List;
import java.util.Set;

public interface ModelOperations {
    List<OntClass> getImplementationClasses();

    /* only implementations in phase */
    List<Implementation> getImplementations(Phase phase);

    /* depends on implementations of implementation in phase phase*/
    List<Implementation> getDependsOnImplementations(Implementation implementation, Phase phase);

    Set<OntologyDependency> getOntologyDependencies(Phase phase, Set<OntologyDependency> alreadySeen);
}
