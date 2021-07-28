package de.fraunhofer.iosb.svs.sae.workflowmanager;

import de.fraunhofer.iosb.svs.sae.FTPClientService;
import de.fraunhofer.iosb.svs.sae.db.*;
import de.fraunhofer.iosb.svs.sae.restclient.sme.TargetSystem;
import de.fraunhofer.iosb.svs.sae.workflowmanager.bpmn.BpmnModel;
import de.fraunhofer.iosb.svs.sae.workflowmanager.bpmn.Nodeable;
import de.fraunhofer.iosb.svs.sae.workflowmanager.datasource.CachedOntModelHandler;
import de.fraunhofer.iosb.svs.sae.workflowmanager.model.Implementation;
import de.fraunhofer.iosb.svs.sae.workflowmanager.model.OntologyDependency;
import de.fraunhofer.iosb.svs.sae.workflowmanager.model.Phase;
import de.fraunhofer.iosb.svs.sae.workflowmanager.model.ProcessingModule;
import de.fraunhofer.iosb.svs.sae.workflowmanager.operations.PolicyModelOperations;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowManager {

    private static final Logger log = LoggerFactory.getLogger(WorkflowManager.class);
    private static final String SUFFIX_OUTPUT_ONTOLOGY = "_Output_Ontology";
    private static final String SYSTEM_MODEL_OUTPUT_ONTOLOGY = "System_Model" + SUFFIX_OUTPUT_ONTOLOGY;

    private final CachedOntModelHandler cachedOntModelHandler;
    private final PolicyAnalysisRepository policyAnalysisRepository;
    private final WorkflowRepository workflowRepository;
    private final FTPClientService ftpClientService;

    @Value("${workflows.dir}")
    String workflowsDir;

    @Autowired
    public WorkflowManager(CachedOntModelHandler cachedOntModelHandler,
            PolicyAnalysisRepository policyAnalysisRepository, WorkflowRepository workflowRepository,
            FTPClientService ftpClientService) {
        this.cachedOntModelHandler = cachedOntModelHandler;
        this.policyAnalysisRepository = policyAnalysisRepository;
        this.workflowRepository = workflowRepository;
        this.ftpClientService = ftpClientService;
    }

    /**
     * Creates Workflows for an Analysis.
     * <p>
     * Prepares the model with implementations and OntologyDependencies from the
     * TargetSystem. Creates a directory for the analysis. Iterates the three phases
     * (Phase.STATIC_KNOWLEDGE_EXTENSION, Phase.DYNAMIC_KNOWLEDGE_EXTENSION,
     * Phase.ANALYSIS): Creates a bpmn workflow for each phase {@link #doBpmn}. Adds
     * the workflow to the analysis. Saves the imageNames from the ProcessingModules
     * (docker) Prepares the uploads for the next phase.
     *
     * @param analysis the analysis to create workflows for
     * @return a set of image names from processing modules, for which worker should
     *         be started
     */
    public Set<String> createWorkflows(Analysis analysis) {
        List<String> graphNames = analysis.getPolicyAnalyses().stream().map(PolicyAnalysis::getGraphName)
                .collect(Collectors.toList());
        TargetSystem targetSystem = analysis.getTargetSystem();
        log.debug("Create workflows using graphs {}", graphNames);
        PolicyModelOperations policyModelOperations = new PolicyModelOperations(graphNames, cachedOntModelHandler);

        Set<OntologyDependency> alreadySeenOntologyDependencies = prepareFromTargetSystem(policyModelOperations,
                targetSystem);
        
        // Check if the workflows dir exists and create it if not
        File workflowsLocation = new File(workflowsDir);
        if(!workflowsLocation.exists()) {
            workflowsLocation.mkdir();
        }
        
        // creating a folder with the policyAnalysisId as the foldername
        File pathName = new File(workflowsDir + "/" + String.valueOf(analysis.getId()));
        if (pathName.mkdir()) {
            log.debug("Directory {} was created", pathName);
        } else if (pathName.exists()) {
            log.warn("Directory {} already exists", pathName);
        } else {
            log.error("Directory {} neither exists nor was created", pathName);
        }

        // processing module image names
        Set<String> imageNames = new HashSet<>();

        // iterating the three phases
        List<Phase> phases = Arrays.asList(Phase.STATIC_KNOWLEDGE_EXTENSION, Phase.DYNAMIC_KNOWLEDGE_EXTENSION,
                Phase.ANALYSIS);
        Map<String, String> uploads = new HashMap<>();
        uploads.put(SYSTEM_MODEL_OUTPUT_ONTOLOGY, targetSystem.getOntologyPath());
        for (Phase phase : phases) {
            // creating bpmn file for workflow phase and updating policy Analysis
            Workflow workflow = doBpmn(phase, String.valueOf(analysis.getId()), policyModelOperations, uploads,
                    alreadySeenOntologyDependencies);
            analysis.addWorkflow(workflow);
            workflowRepository.save(workflow);
            // policyAnalysisRepository.save(policyAnalysis);

            addImageNames(policyModelOperations, imageNames, phase);

            // prepare next phase
            if (!workflow.getOutputOntologyName().contains("serviceUpload")) {
                String outputOntology = ftpClientService.getOntologiesMountpoint() + "/"
                        + workflow.getOutputOntologyName();
                uploads.clear();
                uploads.put(phase.getLocalName() + SUFFIX_OUTPUT_ONTOLOGY, outputOntology);
            }
        }
        return imageNames;
    }

    /**
     * Takes all implementations of a phase and filters it by ProcessingModule. Then
     * takes the image names and adds it to a set.
     *
     * @param policyModelOperations representing the model
     * @param imageNames            the set to add the imageNames to
     * @param phase                 the phase to take the implementations from
     */
    private void addImageNames(PolicyModelOperations policyModelOperations, Set<String> imageNames, Phase phase) {
        // add ProcessingModules imageNames to return set
        policyModelOperations.getImplementationsStream(phase)
                .filter(implementation -> implementation instanceof ProcessingModule)
                .map(implementation -> (ProcessingModule) implementation).map(ProcessingModule::getImageName)
                .collect(Collectors.toCollection(() -> imageNames));
    }

    /**
     * Prepares the contents of the policy analysis on the already containing
     * Implementations and OntologyDependencies of the TargetSystem.
     * <p>
     *
     * @param policyModelOperations representing the model
     * @param targetSystem          the TargetSystem to pull the information from
     * @return a set of OntologyDependencies that have been already introduced in
     *         the TargetSystem
     */
    private Set<OntologyDependency> prepareFromTargetSystem(PolicyModelOperations policyModelOperations,
            TargetSystem targetSystem) {
        // this sets gets filled with all OntologyDependencies that are included in the
        // working ontology
        // if we see implementations or OntologyDependencies that depend on these
        // alreadySeenOntologyDependencies,
        // we do not have to introduce a new mapping task for the ontology represented
        // by the iri in the bpmn
        Set<OntologyDependency> includedOntologyDependencies = new HashSet<>();
        for (String ontologyDependencyId : targetSystem.getOntologyDependencyIds()) {
        	Optional<OntologyDependency> dep = policyModelOperations.getOntologyDependencies().stream().filter(ontologyDependency -> ontologyDependency
        			.getIndividual().getLocalName().equals(ontologyDependencyId)).findAny();
        	if(dep.isPresent()) {
        		includedOntologyDependencies.add(dep.get());
        	}
        }

        // all implementations of the other phases that are not in the task ids, have to
        // be added to static knowledge extension
        // also implementations that have no specific phase.
        List<Phase> precedingPhases = Arrays.asList(Phase.KNOWLEDGE_COLLECTION, Phase.KNOWLEDGE_FUSION,
                Phase.MODEL_CLEANING, Phase.ANY_PHASE);
        for (Phase phase : precedingPhases) {
            convertToStaticKnowledgeCollection(policyModelOperations, phase, targetSystem.getTaskIds());
        }

        return includedOntologyDependencies;
    }

    /**
     * Takes all implementations of this phase and changes their local phase to
     * StaticKnowledgeCollection. Excluding certain implementations.
     *
     * @param policyModelOperations representing the model
     * @param phase                 the phase to modify the implementations
     * @param excludedTaskIds       the task ids, i.e. local names, to exclude
     */
    private void convertToStaticKnowledgeCollection(PolicyModelOperations policyModelOperations, Phase phase,
            Set<String> excludedTaskIds) {
        // take all implementations from the phase
        policyModelOperations.getImplementationsStream(phase)
                // filter those which have their local name not contained in the target system
                .filter(implementation -> !excludedTaskIds.contains(implementation.getLocalName()))
                // change their phase to static knowledge collection
                .forEach(implementation -> implementation.setPhase(Phase.STATIC_KNOWLEDGE_EXTENSION));
        ;
    }

    private Workflow doBpmn(Phase phase, String identifier, PolicyModelOperations policyModelOperations,
            Map<String, String> uploads, Set<OntologyDependency> alreadySeenOntologyDependencies) {
        DirectedAcyclicGraph<Nodeable, DefaultEdge> graph = new DirectedAcyclicGraph<>(DefaultEdge.class);
        // adding all implementations to the graph
        List<Implementation> implementations = policyModelOperations.getImplementations(phase);
        implementations.forEach(graph::addVertex);

        // adding edges for dependsOn relationships between implementations
        // dependsOn: impl->impl
        implementations.forEach(implementation -> {
            policyModelOperations.getDependsOnImplementations(implementation, phase)
                    .forEach(dependent -> graph.addEdge(dependent, implementation));
        });

        // all OntologyDependencies, that a implementation we are using, has a
        // dependsOn. But without those we have already seen -->
        // mergedOntologyDependencies
        Map<Implementation, Set<OntologyDependency>> implementationsWithOntologyDependencies = policyModelOperations
                .getImplementationsWithOntologyDependencies(phase, alreadySeenOntologyDependencies);
        Set<OntologyDependency> mergedOntologyDependencies = implementationsWithOntologyDependencies.values().stream()
                .flatMap(Collection::stream).collect(Collectors.toSet());
        alreadySeenOntologyDependencies.addAll(mergedOntologyDependencies);

        // special case: if this is the analysis phase we also have to consider the
        // ontology dependencies for the used query
        if (phase == Phase.ANALYSIS) {
            Set<OntologyDependency> queryOntologyDependencies = policyModelOperations
                    .getQueryDependsOnOntologyDependencies(alreadySeenOntologyDependencies);
            mergedOntologyDependencies.addAll(queryOntologyDependencies);
        }

        // adding those OntologyDependencies to the graph
        mergedOntologyDependencies.forEach(graph::addVertex);

        // adding edges from all implementations that have a OntologyDependency to that
        // OntologyDependency
        // dependsOn: impl->ontoDep
        implementationsWithOntologyDependencies.forEach((implementation, ontologyDependencies) -> {
            ontologyDependencies.forEach(ontologyDependency -> graph.addEdge(ontologyDependency, implementation));
        });
        // adding edges between the OntologyDependencies if they dependOn another
        // dependsOn: ontoDep->ontoDep
        mergedOntologyDependencies.forEach(ontologyDependency -> {
            policyModelOperations.getDependsOnOntologyDependencies(ontologyDependency, mergedOntologyDependencies)
                    .forEach(dependent -> graph.addEdge(dependent, ontologyDependency));
        });
        String workflowName = "Analysis_" + identifier + "_" + phase.getLocalName();
        BpmnModel bpmnModel = new BpmnModel(uploads, workflowName, ftpClientService);
        bpmnModel.addElementsFromGraph(graph);
        File toSave = new File(workflowsDir + "/" + identifier, workflowName + ".bpmn");
        log.debug("Saving file {} for phase {}", workflowName + ".bpmn", phase.getLocalName());
        bpmnModel.saveToFile(toSave);

        Workflow workflow = new Workflow();
        workflow.setLocalFile(toSave);
        workflow.setPhase(phase);
        workflow.setOutputOntologyName(bpmnModel.getOutputOntologyName());
        workflow.setProcessId(workflowName);
        return workflow;
    }

}
