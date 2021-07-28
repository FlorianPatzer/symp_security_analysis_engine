package de.fraunhofer.iosb.svs.sae.workflowmanager.bpmn;

import de.fraunhofer.iosb.svs.sae.MountpointHandler;
import de.fraunhofer.iosb.svs.sae.workflowmanager.bpmn.nodes.Connector;
import de.fraunhofer.iosb.svs.sae.workflowmanager.model.*;

import com.google.common.collect.Iterables;
import org.apache.jena.ontology.Individual;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.ParallelGatewayBuilder;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.camunda.bpm.model.bpmn.builder.ServiceTaskBuilder;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaInputOutput;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaInputParameter;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.fraunhofer.iosb.svs.sae.workflowmanager.bpmn.FraunhoferTypeCamunda.*;
import static de.fraunhofer.iosb.svs.sae.workflowmanager.model.OntologyDependency.ONTMAPPING_TOPIC;

/**
 * Aims to create a BPMN file with tasks taken from a graph.
 */
public class BpmnModel {

    public static final String ONTREMERGE_TOPIC = "ontremerge";
    public static final String ONTUPLOAD_TOPIC = "ontupload";
    private static final Logger log = LoggerFactory.getLogger(BpmnModel.class);
    private static final String START_EVENT = "Start";
    private static final String END_EVENT = "End";
    /**
     * The created {@link BpmnModelInstance} containing all BPMN information.
     * Is used to get builders for already created elements.
     * Also needed for validating and saving to file.
     */
    private BpmnModelInstance modelInstance;

    /**
     * The builder that is last used.
     * Needed to set the id for the start node.
     * Needed for adding the initial tasks and the end event.
     */
    private AbstractFlowNodeBuilder lastUsed;

    /**
     * The name of the Ontology that will be outputted by this bpmn model. out_{id_last_service_task}.owl
     */
    private String outputOntologyName;

    /**
     * Handler that generates uri for uploaded files. In the ontologies we can only find the file names, so we need to add the path in front
     */
    private final MountpointHandler mountpointHandler;

    /**
     * Takes a map of uploads and a processId to start the BPMN file using the Camunda BPMN Builder
     *
     * @param uploads   the uploads that are added as first task. If there is more than one upload, also a merge task is appended
     * @param processId the processId. Needs to be unique for processes that belong to the same analysis
     */
    public BpmnModel(Map<String, String> uploads, String processId, MountpointHandler mountpointHandler) {
        this.mountpointHandler = mountpointHandler;
        ProcessBuilder processBuilder = Bpmn.createExecutableProcess(processId);
        modelInstance = processBuilder.done();
        modelInstance.getDefinitions().setTargetNamespace("http://bpmn.io/schema/bpmn");
        lastUsed = processBuilder.startEvent(START_EVENT).name("StartEvent");
        lastUsed = addOntologyUploadTask(lastUsed, uploads, "serviceUpload", "Initial Ontology Upload Task");
        if (uploads.entrySet().size() > 1) {
            // more than one upload so we need a remergetask
            lastUsed = handleRemerge(lastUsed, "OntRemerge_initial");
        }
    }

    /**
     * Adds a {@link ServiceTask} with the ONTUPLOAD_TOPIC and the ontupload property to the parentBuilder.
     * Also adds the uploads as input output.
     *
     * @param uploads the uploads to add as input parameters
     * @param id      the id of the new {@link ServiceTask}
     * @param name    the name of the new {@link ServiceTask}
     * @return the builder of the created {@link ServiceTask}
     */
    private ServiceTaskBuilder addOntologyUploadTask(AbstractFlowNodeBuilder parentBuilder, Map<String, String> uploads, String id, String name) {
        return parentBuilder.serviceTask(id)
                .name(name)
                .camundaType("external")
                .camundaTopic(ONTUPLOAD_TOPIC)
                // what property?
                .addExtensionElement(getFraunhoferPropertyEE(ONTUPLOAD))
                .addExtensionElement(getInputOutputEE(uploads));
    }

    /**
     * Creates a {@link CamundaProperties} containing a {@link CamundaProperty} with the id "fraunhofer-type".
     *
     * @param value the value the fraunhofer-type property should have
     * @return the created extension element
     */
    private CamundaProperties getFraunhoferPropertyEE(String value) {
        CamundaProperties camundaProperties = modelInstance.newInstance(CamundaProperties.class);
        CamundaProperty camundaProperty = modelInstance.newInstance(CamundaProperty.class);
        camundaProperty.setCamundaId("fraunhofer-type");
        camundaProperty.setCamundaName("type");
        camundaProperty.setCamundaValue(value);
        camundaProperties.addChildElement(camundaProperty);
        return camundaProperties;
    }

    /**
     * Creates a {@link CamundaInputOutput} with a {@link CamundaInputParameter}s having a name and a text content.
     *
     * @param contentMap the map containing name, text content pairs for the input parameters
     * @return the created extension element
     */
    private CamundaInputOutput getInputOutputEE(Map<String, String> contentMap) {
        CamundaInputOutput camundaInputOutput = modelInstance.newInstance(CamundaInputOutput.class);
        contentMap.forEach((name, content) -> {
            CamundaInputParameter camundaInputParameter = modelInstance.newInstance(CamundaInputParameter.class);
            camundaInputParameter.setCamundaName(name);
            camundaInputParameter.setTextContent(content);
            camundaInputOutput.addChildElement(camundaInputParameter);
        });
        return camundaInputOutput;
    }

    /**
     * Creates a {@link CamundaInputOutput} with a {@link CamundaInputParameter} having a name and a text content.
     *
     * @param name    the name of the input parameter
     * @param content the content of the input parameter
     * @return the created extension element
     */
    private CamundaInputOutput getInputOutputEE(String name, String content) {
        CamundaInputOutput camundaInputOutput = modelInstance.newInstance(CamundaInputOutput.class);
        CamundaInputParameter camundaInputParameter = modelInstance.newInstance(CamundaInputParameter.class);
        camundaInputParameter.setCamundaName(name);
        camundaInputParameter.setTextContent(content);
        camundaInputOutput.addChildElement(camundaInputParameter);
        return camundaInputOutput;
    }

    /**
     * Adds a {@link ServiceTask} to the parentBuilder.
     * Depending on which kind of implementation it is, add topic, property and potential input outputs.
     *
     * @param parentBuilder  the builder to add the new {@link ServiceTask} to
     * @param implementation the implementation corresponding to the created task
     * @param id             the id of the new {@link ServiceTask}
     * @return the builder of the created {@link ServiceTask}
     */
    private ServiceTaskBuilder handleImplementation(AbstractFlowNodeBuilder parentBuilder, Implementation implementation, String id) {
        ServiceTaskBuilder serviceTaskBuilder = parentBuilder.serviceTask(id).name(implementation.getLocalName()).camundaType("external");
        if (implementation instanceof ProcessingModule) {
            ProcessingModule processingModule = (ProcessingModule) implementation;
            // processing module has imageName, we add it as input output
            serviceTaskBuilder
                    .camundaTopic(processingModule.getTopicName())
                    .addExtensionElement(getFraunhoferPropertyEE(PROCESSINGMODULE))
                    .addExtensionElement(getInputOutputEE("ImageName", processingModule.getImageName()));
        } else if (implementation instanceof ProcessingRule) {
            ProcessingRule processingRule = (ProcessingRule) implementation;
            serviceTaskBuilder.addExtensionElement(getInputOutputEE("Rule", processingRule.getRule()));
            serviceTaskBuilder.camundaTopic(processingRule.getTopicName()); // should be jena or swrl respectively
            if (processingRule instanceof JenaRule) {
                serviceTaskBuilder
                        .addExtensionElement(getFraunhoferPropertyEE(JENA));
            } else if (implementation instanceof SwrlRule) {
                serviceTaskBuilder
                        .addExtensionElement(getFraunhoferPropertyEE(SWRL));
            }
        }
        return serviceTaskBuilder;
    }

    private ServiceTaskBuilder handleCombination(AbstractFlowNodeBuilder parentBuilder, Combination combination, String id) {
        String name = combination.getRules().stream().map(ProcessingRule::getLocalName).collect(Collectors.joining("_"));
        ServiceTaskBuilder serviceTaskBuilder = parentBuilder.serviceTask(id).name("Rule Merge: " + name).camundaType("external");
        List<? extends ProcessingRule> rules = combination.getRules();
        serviceTaskBuilder.camundaTopic(rules.get(0).getTopicName());
        log.debug("Size: {}", rules.size());
        Map<String, String> rulesAsInputs = IntStream.range(0, rules.size())
                .boxed()
                .collect(Collectors.toMap(index -> "Rule_" + index, index -> rules.get(index).getRule()));
        serviceTaskBuilder.addExtensionElement(getInputOutputEE(rulesAsInputs));
        if (combination instanceof SwrlCombination) {
            serviceTaskBuilder
                    .addExtensionElement(getFraunhoferPropertyEE(SWRL));
        } else if (combination instanceof JenaCombination) {
            serviceTaskBuilder
                    .addExtensionElement(getFraunhoferPropertyEE(JENA));
        }
        return serviceTaskBuilder;
    }

    /**
     * Adds a {@link ServiceTask} with the ONTMAPPING_TOPIC and the ontmapping property to the parentBuilder.
     * Also adds the ontology namespace uri as input parameter.
     *
     * @param parentBuilder      the builder to add the new {@link ServiceTask} to
     * @param ontologyDependency the ontology dependency corresponding to the created task
     * @param id                 the id of the new {@link ServiceTask}
     * @return the builder of the created {@link ServiceTask}
     */
    private ServiceTaskBuilder handleOntologyDependency(AbstractFlowNodeBuilder parentBuilder, OntologyDependency ontologyDependency, String id) {
        Individual individual = ontologyDependency.getIndividual();
        String ontologyFilePath = mountpointHandler.getUriFor(mountpointHandler.getFilenameForOwl(ontologyDependency.getPrefix()));
        return parentBuilder
                .serviceTask(id)
                .name(ontologyDependency.getLocalName())
                .camundaType("external")
                .camundaTopic(ONTMAPPING_TOPIC)
                .addExtensionElement(getFraunhoferPropertyEE(ONTMAPPING))
                .addExtensionElement(getInputOutputEE(ontologyDependency.getPrefix(), ontologyFilePath));
    }

    /**
     * Adds a parallel gateway to the parent builder
     *
     * @param parentBuilder the builder to add the new parallel gateway to
     * @param id            the id of the new parallel gateway
     * @return the builder of the created parallel gateway
     */
    private ParallelGatewayBuilder handleFork(AbstractFlowNodeBuilder parentBuilder, String id) {
        return parentBuilder.parallelGateway(id);
    }

    /**
     * Adds a parallel gateway and connects all parent builders to it.
     *
     * @param parentBuilders the set of builders that will be connected to the created parallel gateway
     * @param id             the id of the new parallel gateway
     * @return the builder of the created parallel gateway
     */
    private ParallelGatewayBuilder handleJoin(Set<AbstractFlowNodeBuilder> parentBuilders, String id) {
        Iterator<AbstractFlowNodeBuilder> parentIterator = parentBuilders.iterator();
        // with first item we need to create the parallel gateway. The others just connect to it
        ParallelGatewayBuilder parallelGatewayBuilder = null;
        if (parentIterator.hasNext()) {
            AbstractFlowNodeBuilder builder = parentIterator.next();
            parallelGatewayBuilder = builder.parallelGateway(id);
        } else {
            log.warn("No parents for gateway '{}', returning null gateway builder ", id);
        }
        while (parentIterator.hasNext()) {
            AbstractFlowNodeBuilder builder = parentIterator.next();
            builder.connectTo(id);
        }
        return parallelGatewayBuilder;
    }

    /**
     * Adds a {@link ServiceTask} with the ONTREMERGE_TOPIC and the ontmerge property to the parentBuilder.
     *
     * @param parentBuilder the builder to add the new {@link ServiceTask} to
     * @param id            the id of the new {@link ServiceTask}
     * @return the builder of the created {@link ServiceTask}
     */
    private ServiceTaskBuilder handleRemerge(AbstractFlowNodeBuilder parentBuilder, String id) {
        return parentBuilder
                .serviceTask(id)
                .camundaType("external")
                .camundaTopic(ONTREMERGE_TOPIC)
                .addExtensionElement(getFraunhoferPropertyEE(ONTMERGE));
    }

    /**
     * Uses a {@link ScheduleAlgorithm} to put the tasks of the graph in a suiting order.
     * Proceeds to use a topological order to iterate the resulting graph and adding the corresponding BPMN tasks using the BPMN builder.
     *
     * @param graph the graph which is first realigned and then used for creating tasks
     */
    public void addElementsFromGraph(DirectedAcyclicGraph<Nodeable, DefaultEdge> graph) {
        ScheduleAlgorithm scheduleAlgorithm = new ScheduleAlgorithm(graph);
        DirectedAcyclicGraph<Node, DefaultEdge> scheduleGraph = scheduleAlgorithm.getScheduleGraph();

        Iterator<Node> iterator = new TopologicalOrderIterator<>(scheduleGraph);
        while (iterator.hasNext()) {
            Node node = iterator.next();
            Nodeable nodeable = node.getNodeable();
            // TODO these should be in classes with a method of interface nodeable
            if (nodeable instanceof Implementation) {
                Implementation implementation = (Implementation) nodeable;
                // every node that is a content node has only one parent
                // also parent already exists, because of topological order
                AbstractFlowNodeBuilder parentBuilder = getSingleParentBuilder(scheduleGraph, node);
                lastUsed = handleImplementation(parentBuilder, implementation, node.getId());
            } else if (nodeable instanceof OntologyDependency) {
                OntologyDependency ontologyDependency = (OntologyDependency) nodeable;
                // every node that is a content node has only one parent
                // also parent already exists, because of topological order
                AbstractFlowNodeBuilder parentBuilder = getSingleParentBuilder(scheduleGraph, node);
                lastUsed = handleOntologyDependency(parentBuilder, ontologyDependency, node.getId());
            } else if (nodeable instanceof Combination) {
                Combination combination = (Combination) nodeable;
                // every node that is a content node has only one parent
                // also parent already exists, because of topological order
                AbstractFlowNodeBuilder parentBuilder = getSingleParentBuilder(scheduleGraph, node);
                lastUsed = handleCombination(parentBuilder, combination, node.getId());
            } else if (node.is(Connector.FORK)) {
                // every node that is a fork node has only one parent
                // also parent already exists, because of topological order
                AbstractFlowNodeBuilder parentBuilder = getSingleParentBuilder(scheduleGraph, node);
                lastUsed = handleFork(parentBuilder, node.getId());
            } else if (node.is(Connector.JOIN)) {
                // every node that is a join node has multiple parent
                Set<DefaultEdge> incomings = scheduleGraph.incomingEdgesOf(node);
                Set<Node> parents = incomings.stream().map(scheduleGraph::getEdgeSource)
                        .collect(Collectors.toSet());
                // also parent already exists, because of topological order
                Set<AbstractFlowNodeBuilder> parentBuilders = parents.stream()
                        .map(parent -> ((FlowNode) modelInstance.getModelElementById(parent.getId())).builder()).collect(Collectors.toSet());
                lastUsed = handleJoin(parentBuilders, node.getId());
            } else if (node.is(Connector.START)) {
                // last used should be upload or merge task, set id of start node to this
                String id = lastUsed.getElement().getAttributeValue("id");
                log.debug("ID value is {}", id);
                node.setId(id);
            } else if (node.is(Connector.REMERGE)) {
                // every node that is a merge node has only one parent
                AbstractFlowNodeBuilder parentBuilder = getSingleParentBuilder(scheduleGraph, node);
                lastUsed = handleRemerge(parentBuilder, node.getId());
            }
        }
        // last Used is last service task which determines the name of the resulting ontology
        //this.outputOntologyName = "out_" + lastUsed.getElement().getAttributeValue("id") + ".owl";
        this.outputOntologyName = lastUsed.getElement().getAttributeValue("id") + ".owl";
        lastUsed.endEvent(END_EVENT);
    }

    /**
     * Gets the AbstractFlowNodeBuilder for the single parent of a node.
     * <p>
     * Assuming only one parent exists.
     * Assuming the builder already exists, because the parent got handled, because of topological order
     *
     * @param scheduleGraph
     * @param node
     * @return
     */
    private AbstractFlowNodeBuilder getSingleParentBuilder(DirectedAcyclicGraph<Node, DefaultEdge> scheduleGraph, Node node) {
        Node parent = getSingleParentNode(scheduleGraph, node);
        return ((FlowNode) modelInstance.getModelElementById(parent.getId())).builder();
    }

    /**
     * Assuming the node has only one parent, return that parent.
     *
     * @param scheduleGraph
     * @param node
     * @return
     */
    private Node getSingleParentNode(DirectedAcyclicGraph<Node, DefaultEdge> scheduleGraph, Node node) {
        DefaultEdge incoming = Iterables.getOnlyElement(scheduleGraph.incomingEdgesOf(node));
        return scheduleGraph.getEdgeSource(incoming);
    }

    /**
     * Gets the name of the ontology that will be outputted by this {@link BpmnModel}.
     * <p>
     * Is only filled when {@link #addElementsFromGraph} has been run. Otherwise null.
     * <p>
     * out_{id_last_service_task}.owl
     *
     * @return the output ontology name
     */
    public String getOutputOntologyName() {
        return outputOntologyName;
    }

    /**
     * Adds an {@link org.camunda.bpm.model.bpmn.instance.EndEvent} to the last node, validates the model an then saves it to a file.
     *
     * @param file the filename used. Should already contain '.bpmn'
     * @return itself
     */
    public BpmnModel saveToFile(File file) {
        log.debug("Validate model");
        Bpmn.validateModel(modelInstance);
        log.debug("Write model to file {}", file);
        Bpmn.writeModelToFile(file, modelInstance);
        return this;
    }

}
