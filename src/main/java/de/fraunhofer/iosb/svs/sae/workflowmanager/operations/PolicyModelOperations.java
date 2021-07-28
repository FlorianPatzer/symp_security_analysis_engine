package de.fraunhofer.iosb.svs.sae.workflowmanager.operations;

import de.fraunhofer.iosb.svs.sae.workflowmanager.datasource.OntModelGetter;
import de.fraunhofer.iosb.svs.sae.workflowmanager.model.*;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.fraunhofer.iosb.svs.sae.workflowmanager.model.Implementation.*;
import static de.fraunhofer.iosb.svs.sae.workflowmanager.model.ObjectProperty.DEPENDS_ON_URI;
import static de.fraunhofer.iosb.svs.sae.workflowmanager.model.OntologyDependency.ONTOLOGY_DEPENDENCY_URI;
import static de.fraunhofer.iosb.svs.sae.workflowmanager.model.Policy.POLICY_BASED_ANALYSIS_URI_VERSION;
import static de.fraunhofer.iosb.svs.sae.workflowmanager.model.Query.ANALYTIC_QUERY_URI;

public class PolicyModelOperations implements ModelOperations {
    private static final Logger log = LoggerFactory.getLogger(PolicyModelOperations.class);
    private final OntClass ontologyDependencyClass;
    private final Property dependsOnProperty;
    private final OntClass implementationClass;
    private List<Implementation> implementations;
    private Set<OntologyDependency> ontologyDependencies;
    private Set<Query> queries;

    public PolicyModelOperations(List<String> modelNames, OntModelGetter ontModelGetter) {
        // fetch policy based analysis from file
        OntModel policyBasedAnalysis = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        InputStream in = RDFDataMgr.open("ontologies/policy-based-analysis.owl");
        policyBasedAnalysis.read(in, "RDF/XML");
        OntDocumentManager.getInstance().addModel(POLICY_BASED_ANALYSIS_URI_VERSION, policyBasedAnalysis);
        // fetch models
        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
        for (String modelName : modelNames) {
            ontModel.add(ontModelGetter.getOntModel(modelName));
        }
        implementationClass = ontModel.getOntClass(IMPLEMENTATION_URI);
        ontologyDependencyClass = ontModel.getOntClass(ONTOLOGY_DEPENDENCY_URI);
        dependsOnProperty = ontModel.getProperty(DEPENDS_ON_URI);
        this.implementations = getAllImplementations(ontModel);
        this.ontologyDependencies = getAllOntologyDependencies(ontModel);
        this.queries = getQueries(ontModel);
    }

    private List<Implementation> getAllImplementations(OntModel ontModel) {
        /*this.implementations = implementationClass.listInstances()*/
        List<Implementation> implementations = new ArrayList<>();
        implementations.addAll(ontModel.getOntClass(PROCESSING_MODULE_URI).listInstances()
                .mapWith(instance -> new ProcessingModule(instance.asIndividual())).toList());
        implementations.addAll(ontModel.getOntClass(JENA_RULE_URI).listInstances()
                .mapWith(instance -> new JenaRule(instance.asIndividual())).toList());
        implementations.addAll(ontModel.getOntClass(SWRL_RULE_URI).listInstances()
                .mapWith(instance -> new SwrlRule(instance.asIndividual())).toList());
        return implementations;
    }

    private Set<OntologyDependency> getAllOntologyDependencies(OntModel ontModel) {
        return ontModel.getOntClass(ONTOLOGY_DEPENDENCY_URI).listInstances().mapWith(instance -> new OntologyDependency(instance.asIndividual())).toSet();
    }

    private Set<Query> getQueries(OntModel ontModel) {
        Set<Query> queries = new HashSet<>();
        // only analytic queries for now
        queries.addAll(ontModel.getOntClass(ANALYTIC_QUERY_URI).listInstances().mapWith(instance -> new AnalyticQuery(instance.asIndividual())).toSet());
        return queries;
    }

    @Override
    public List<OntClass> getImplementationClasses() {
        implementationClass.listSubClasses().toList().forEach(clas -> log.debug(clas.toString()));
        return implementationClass.listSubClasses().toList();
    }

    public List<Implementation> getImplementations() {
        return this.implementations;
    }

    public Stream<Implementation> getImplementationsStream(Phase phase) {
        return implementations.stream().filter(implementation ->
                implementation.getPhase() == phase
        );
    }

    @Override
    public List<Implementation> getImplementations(Phase phase) {
        return getImplementationsStream(phase).collect(Collectors.toList());
    }

    public List<Implementation> getDependsOnImplementations(Implementation implementation) {
        return getDependsOnImplementationsStream(implementation).toList();
    }

    private ExtendedIterator<Implementation> getDependsOnImplementationsStream(Implementation implementation) {
        // get all Nodes we have a dependsOn on
        // cast them as individuals and keep only implementations
        // map each individual to an implementation by searching the implementations list for an implementation with that individual
        return implementation.getIndividual()
                .listPropertyValues(dependsOnProperty)
                .mapWith(rdfNode -> rdfNode.as(Individual.class))
                .filterKeep(individual -> individual.hasRDFType(implementationClass))
                .mapWith(individual ->
                        implementations.stream()
                                .filter(impl -> impl.hasIndividual(individual))
                                .findAny()
                                .orElseThrow(() -> new NoSuchElementException("No Implementation with individual found")));
    }

    @Override
    public List<Implementation> getDependsOnImplementations(Implementation implementation, Phase phase) {
        return getDependsOnImplementationsStream(implementation).filterKeep(implementation1 ->
                implementation1.getPhase() == phase).toList();
    }

    public Set<OntologyDependency> getOntologyDependencies() {
        return this.ontologyDependencies;
    }

    public Map<Implementation, Set<OntologyDependency>> getImplementationsWithOntologyDependencies(Phase phase, Set<OntologyDependency> alreadySeen) {
        // stream all implementations in this phase
        return getImplementationsStream(phase)
                .collect(Collectors.toMap(implementation -> implementation, (implementation ->
                        // map implementations to a set of all ontologyDependencies it has a dependsOn
                        implementation.getIndividual().listPropertyValues(dependsOnProperty)
                                .mapWith(rdfNode -> rdfNode.as(Individual.class))
                                .filterKeep(individual -> individual.hasRDFType(ontologyDependencyClass))
                                // need to look up individuals in our ontologyDependency list and find any, that fits that individual
                                .mapWith(individual -> ontologyDependencies.stream()
                                        .filter(ontologyDependency -> ontologyDependency.hasIndividual(individual))
                                        .findAny()
                                        .orElseThrow(() -> new NoSuchElementException("No OntologyDependency with individual found")))
                                .filterDrop(alreadySeen::contains)
                                .toSet())));
    }

    @Override
    public Set<OntologyDependency> getOntologyDependencies(Phase phase, Set<OntologyDependency> alreadySeen) {
        // stream all implementations in this phase
        return getImplementationsStream(phase).map(implementation ->
                // map implementations to a set of all ontologyDependencies it has a dependsOn
                implementation.getIndividual().listPropertyValues(dependsOnProperty)
                        .mapWith(rdfNode -> rdfNode.as(Individual.class))
                        .filterKeep(individual -> individual.hasRDFType(ontologyDependencyClass))
                        // need to look up individuals in our ontologyDependency list and find any, that fits that individual
                        .mapWith(individual -> ontologyDependencies.stream()
                                .filter(ontologyDependency -> ontologyDependency.hasIndividual(individual))
                                .findAny()
                                .orElseThrow(() -> new NoSuchElementException("No OntologyDependency with individual found")))
                        .toSet())
                // flattening the sets to a stream of ontology dependencies
                .flatMap(Collection::stream)
                .filter(ontologyDependency -> !alreadySeen.contains(ontologyDependency))
                .collect(Collectors.toSet());
    }

    /**
     * @param ontologyDependency
     * @param targets
     * @return
     */
    public Set<OntologyDependency> getDependsOnOntologyDependencies(OntologyDependency ontologyDependency, Set<OntologyDependency> targets) {
        // take all depends on
        return ontologyDependency.getIndividual().listPropertyValues(dependsOnProperty)
                .mapWith(rdfNode -> rdfNode.as(Individual.class))
                // only keep ontology dependencies
                .filterKeep(individual -> individual.hasRDFType(ontologyDependencyClass))
                // keep those that have a match in the targetset
                .filterKeep(individual -> targets.stream().anyMatch(target -> target.hasIndividual(individual)))
                // need to look up individuals in our ontologyDependency list and find any, that fits that individual
                .mapWith(individual -> ontologyDependencies.stream()
                        .filter(ontologyDependency1 -> ontologyDependency1.hasIndividual(individual))
                        .findAny()
                        .orElseThrow(() -> new NoSuchElementException("No OntologyDependency with individual found")))
                .toSet();
    }

    public Set<OntologyDependency> getQueryDependsOnOntologyDependencies(Set<OntologyDependency> alreadySeen) {
        // take all depends on
        return queries.stream().map(query ->
                query.getIndividual().listPropertyValues(dependsOnProperty)
                        .mapWith(rdfNode -> rdfNode.as(Individual.class))
                        // only keep ontology dependencies
                        .filterKeep(individual -> individual.hasRDFType(ontologyDependencyClass))
                        // need to look up individuals in our ontologyDependency list and find any, that fits that individual
                        .mapWith(individual -> ontologyDependencies.stream()
                                .filter(ontologyDependency -> ontologyDependency.hasIndividual(individual))
                                .findAny()
                                .orElseThrow(() -> new NoSuchElementException("No OntologyDependency with individual found")))
                        .toSet())
                // flattening the sets to a stream of ontology dependencies
                .flatMap(Collection::stream)
                // remove alreadySeen
                .filter(ontologyDependency -> !alreadySeen.contains(ontologyDependency))
                .collect(Collectors.toSet());
    }
}
