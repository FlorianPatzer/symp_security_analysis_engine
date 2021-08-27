package de.fraunhofer.iosb.svs.sae.workflowmanager.bpmn;

import de.fraunhofer.iosb.svs.sae.workflowmanager.bpmn.nodes.Connector;
import de.fraunhofer.iosb.svs.sae.workflowmanager.model.*;

import com.google.common.collect.Iterables;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Algorithm to put nodes with dependencies on each other into a structure that can be converted to a BPMN workflow.
 */
public class ScheduleAlgorithm {
    private static final Logger log = LoggerFactory.getLogger(ScheduleAlgorithm.class);

    /**
     * The input graph to the algorithm.
     */
    private final DirectedAcyclicGraph<Nodeable, DefaultEdge> graph;
    /**
     * The created graph with the right scheduling.
     */
    private final DirectedAcyclicGraph<Node, DefaultEdge> scheduleGraph;

    /**
     * Creates a new ScheduleAlgorithm.
     * Sets the original graph and creates the new one.
     * Adds a start node.
     * Searches for roots and handles the nodes, i.e. starting the algorithm.
     *
     * @param graph the original graph containing {@link Nodeable}s
     */
    public ScheduleAlgorithm(DirectedAcyclicGraph<Nodeable, DefaultEdge> graph) {
        this.graph = graph;
        scheduleGraph = new DirectedAcyclicGraph<>(DefaultEdge.class);
        // pre calc alternative
        // this is an alternative to detecting mergable rules as done below
        /*Set<List<SwrlRule>> combinationsSwrl = graph.vertexSet().stream().filter(nodeable -> nodeable instanceof SwrlRule).map(nodeable -> (SwrlRule) nodeable)
                .collect(Collectors.groupingBy(graph::incomingEdgesOf))
                .values().stream()
                .filter(swrlRules -> swrlRules.size() > 1).collect(Collectors.toSet());

        for (List<SwrlRule> swrlList : combinationsSwrl) {
            log.debug("New swrl combination found of size {}", swrlList.size());
            SwrlCombination swrlCombination = new SwrlCombination(swrlList);
            graph.addVertex(swrlCombination);
            // set incoming edges
            // they are equal for all swrl rules we combine, so we only need to do it once
            graph.incomingEdgesOf(swrlList.get(0)).stream().map(edge -> graph.getEdgeSource(edge)).forEach(vertex -> graph.addEdge(vertex, swrlCombination));
            swrlList.forEach(swrlRule -> {
                // add outgoing edges to new nodeable
                graph.outgoingEdgesOf(swrlRule).stream().map(edge -> graph.getEdgeSource(edge)).forEach(vertex -> graph.addEdge(swrlCombination, vertex));
                // remove vertex from set
                graph.removeVertex(swrlRule);
            });
        }*/
        log.debug("Calculate schedule graph");

        // first node and only root is always the start node
        Node node = new Node(Connector.START);
        scheduleGraph.addVertex(node);

        // search for roots in the original graph and add them
        Set<Nodeable> roots = getRoots();
        if (roots.isEmpty()) {
            if (graph.vertexSet().isEmpty()) {
                // empty graph
                log.debug("Graph is empty");
            } else {
                throw new IllegalArgumentException("No DAG because no root, but nodes in graph");

            }
        } else {
            Intermediate intermediate = handleScheduled(roots, node, roots);
            log.debug("Last node: " + intermediate.getLast());
            log.debug("Finished: " + intermediate.getTodos().isEmpty());
        }
    }

    /**
     * Checks for a set of todos if they can be added as children to node.
     * Checking is done by looking if the parents (the nodeables, this nodeable has a dependsOn) are in the ancestors of the current node or the node itself.
     * The nodeables that have all dependencies satisfied can be forwarded to {@link #handleScheduled}.
     *
     * @param todos the todos we want to add but have to check first
     * @param node  the node after which the todos want to be placed
     * @return an {@link Intermediate} forwarded from {@link #handleScheduled}
     */
    private Intermediate calculateNode(Set<Nodeable> todos, Node node) {
        Set<Nodeable> scheduleTodos = new HashSet<>();
        for (Nodeable todo : todos) {
            // check if all dependencies for todo are in this path
            Set<Nodeable> parentsOfTodo = getOriginalParents(todo);
            Set<Node> ancestorsOfNode = scheduleGraph.getAncestors(node);
            // want ancestors for potential children so we need to add node to ancestors
            ancestorsOfNode.add(node);
            // if all parents of todo are in ancestors of node and node itself, we can add the todo to the schedule
            if (containsAll(ancestorsOfNode, parentsOfTodo)) {
                scheduleTodos.add(todo);
            }
        }
        return handleScheduled(todos, node, scheduleTodos);
    }

    /**
     * Handles scheduled todos.
     * First update the todos by removing those we are going to handle.
     * <p>
     * Three possibilities:
     * <ul>
     *     <li>No todo is scheduled:
     *     There are no possible nodes to add after the current node,
     *     so an {@link Intermediate} is returned, containing the todos which are still open and the current node
     *     as the last node that was added.
     *     <li>Exactly one todo is scheduled:
     *     The only scheduled node can be added right behind the current node and should be handled next.
     *     This is done by {@link #addAndCalculateNode}.
     *     Passes the retrieved {@link Intermediate} takes all current todos,
     *     removing those that were being handled and adding those which are still open (i.e. being returned)
     *     <li>Multiple todos are scheduled:
     *     First check whether there are rules that can be merged. If so add a new combinations nodeable and remove the rules<p>
     *     Multiple scheduled todos should run parallel. Therefore a Fork node is added.
     *     Then each todo is handled using {@link #addAndCalculateNode}.
     *     After handling all todos a join has to be placed an connected to the last added node of every branch that was created.
     *     This is done by {@link #addAndCalculateJoin} using the gathered highest node (the last in each branch).
     *     Finally the retrieved {@link Intermediate} is passed removing all aggregated Children as well as aggregated todos because these were handled.
     * </ul>
     *
     * @param allTodos      all current open todos
     * @param currentNode   the current node that is operated on
     * @param scheduleTodos the todos that are valid to put after the current node
     * @return an {@link Intermediate}, see method description
     */
    private Intermediate handleScheduled(Set<Nodeable> allTodos, Node currentNode, Set<Nodeable> scheduleTodos) {
        // missing todos is current todos minus the scheduled
        Set<Nodeable> missingTodos = new HashSet<>(allTodos);
        missingTodos.removeAll(scheduleTodos);
        if (scheduleTodos.size() == 0) {
            // return this node an its open todos
            return new Intermediate(allTodos, currentNode);
        } else if (scheduleTodos.size() == 1) {
            // new todos is missingTodos plus all new todos of the handled node
            // there is no need to pass the todos here because if they can be added after the newly created node
            // they have to be a child of that node or a child of a child and then they will be added to todos eventually
            Nodeable nodeable = Iterables.getOnlyElement(scheduleTodos);
            Set<Nodeable> children = getOriginalChildren(nodeable);
            Intermediate intermediate = addAndCalculateNode(children, nodeable, currentNode);
            missingTodos.removeAll(children);
            missingTodos.addAll(intermediate.getTodos());
            return new Intermediate(missingTodos, intermediate.getLast());
        } else {
            // if there are multiple instances of swrl or jena rules,
            // that are about to be put in separate tasks to be run in parallel
            // they need to be combined into single tasks with multiple rules respectively
            List<SwrlRule> swrlList = scheduleTodos.stream()
                    .filter(scheduleTodo -> scheduleTodo instanceof SwrlRule)
                    .map(nodeable -> (SwrlRule) nodeable)
                    .collect(Collectors.toList());
            List<JenaRule> jenaList = scheduleTodos.stream()
                    .filter(scheduleTodo -> scheduleTodo instanceof JenaRule)
                    .map(nodeable -> (JenaRule) nodeable)
                    .collect(Collectors.toList());
            if (swrlList.size() > 1 || jenaList.size() > 1) {
                // at least one possible combination found
                Set<Nodeable> newAllTodos = new HashSet<>(allTodos);
                Set<Nodeable> newScheduleTodos = new HashSet<>(scheduleTodos);
                if (swrlList.size() > 1) {
                    log.debug("Found more than one parallel swrl rule: '{}'. Combining", swrlList.size());
                    SwrlCombination swrlCombination = new SwrlCombination(swrlList);
                    replaceListWithCombination(swrlList, swrlCombination);

                    // need to update todos by removing the replaced nodeables and adding the newly created todo
                    newAllTodos.removeAll(swrlList);
                    newAllTodos.add(swrlCombination);
                    newScheduleTodos.removeAll(swrlList);
                    newScheduleTodos.add(swrlCombination);
                }
                if (jenaList.size() > 1) {
                    log.debug("Found more than one parallel jena rule: '{}'. Combining", jenaList.size());
                    JenaCombination jenaCombination = new JenaCombination(jenaList);
                    replaceListWithCombination(jenaList, jenaCombination);

                    // need to update todos by removing the replaced nodeables and adding the newly created todo
                    newAllTodos.removeAll(jenaList);
                    newAllTodos.add(jenaCombination);
                    newScheduleTodos.removeAll(jenaList);
                    newScheduleTodos.add(jenaCombination);
                }
                // recursive call with updated sets
                return handleScheduled(newAllTodos, currentNode, newScheduleTodos);
            }

            // need to make a fork, handle all scheduled Todos and then put a join at the end
            Node fork = new Node(Connector.FORK);
            log.debug("Add node {}", fork);
            scheduleGraph.addVertex(fork);
            log.debug("Add Edge from {} to {}}", currentNode, fork);
            scheduleGraph.addEdge(currentNode, fork);
            Set<Nodeable> aggregatedTodos = new HashSet<>();
            Set<Nodeable> aggregatedChildren = new HashSet<>();
            Set<Node> aggregatedHighest = new HashSet<>();
            for (Nodeable todo : scheduleTodos) {
                Set<Nodeable> children = getOriginalChildren(todo);
                Intermediate intermediate = addAndCalculateNode(children, todo, fork);
                aggregatedChildren.addAll(children);
                aggregatedTodos.addAll(intermediate.getTodos());
                aggregatedHighest.add(intermediate.getLast());
            }
            // new todos is missingTodos minus all handled todos plus all new todos of the handled todos
            Intermediate intermediate = addAndCalculateJoin(aggregatedTodos, aggregatedHighest);
            missingTodos.removeAll(aggregatedChildren);
            missingTodos.removeAll(aggregatedTodos);
            missingTodos.addAll(intermediate.getTodos());
            return new Intermediate(missingTodos, intermediate.getLast());
        }
    }

    private void replaceListWithCombination(List<? extends ProcessingRule> processingRules, Combination combination) {
        graph.addVertex(combination);
        // set incoming edges
        // they are equal for all rules we combine, so we only need to do it once
        graph.incomingEdgesOf(processingRules.get(0)).stream().map(graph::getEdgeSource).forEach(vertex -> graph.addEdge(vertex, combination));
        processingRules.forEach(rule -> {
            // add outgoing edges to new nodeable
            graph.outgoingEdgesOf(rule).stream().map(graph::getEdgeTarget).forEach(vertex -> graph.addEdge(combination, vertex));
            // remove vertex from set
            graph.removeVertex(rule);
        });
    }

    /**
     * Creates a new node, adds it to the {@link #scheduleGraph} and adds an edge from the parent to the node.
     * After adding, calls {@link #calculateNode}.
     *
     * @param children the children of the nodeable
     * @param nodeable the nodeable that should be added as a node
     * @param parent   the parent node to put a vertex to
     * @return passing {@link #calculateNode}
     */
    private Intermediate addAndCalculateNode(Set<Nodeable> children, Nodeable nodeable, Node parent) {
        Node node = new Node(nodeable);
        log.debug("Add node {}", node);
        scheduleGraph.addVertex(node);
        log.debug("Add Edge from {} to {}}", parent, node);
        scheduleGraph.addEdge(parent, node);
        return calculateNode(children, node);
    }

    /**
     * Creates a new join node and adds it to the {@link #scheduleGraph}.
     * Adds edges from each parent to the join node.
     * Creates a new merge node, adds it to the {@link #scheduleGraph} and adds an edge from the join to the merge node.
     * Calls {@link #calculateNode}.
     *
     * @param todos   the todos that are open
     * @param parents the parents the join should connect to
     * @return passing {@link #calculateNode}
     */
    private Intermediate addAndCalculateJoin(Set<Nodeable> todos, Set<Node> parents) {
        Node join = new Node(Connector.JOIN);
        log.debug("Add node {}", join);
        scheduleGraph.addVertex(join);
        parents.forEach(parent -> {
            log.debug("Add Edge from {} to {}", parent, join);
            scheduleGraph.addEdge(parent, join);
        });
        // add merge node
        Node merge = new Node(Connector.REMERGE);
        scheduleGraph.addVertex(merge);
        scheduleGraph.addEdge(join, merge);
        return calculateNode(todos, merge);
    }

    /**
     * Check if all itemsToCheck are in the target set
     *
     * @param targetSet    the targetSet
     * @param itemsToCheck the items that should be in the targetSet
     * @return true if all itemsToCheck are in the target set false otherwise
     */
    private boolean containsAll(Set<Node> targetSet, Set<Nodeable> itemsToCheck) {
        // are all itemsToCheck in target Set
        return targetSet.stream()
                .map(Node::getNodeable)
                .collect(Collectors.toSet())
                .containsAll(itemsToCheck);
    }

    /**
     * Gets the calculated schedule graph after processing the algorithm
     *
     * @return the schedule graph
     */
    public DirectedAcyclicGraph<Node, DefaultEdge> getScheduleGraph() {
        return scheduleGraph;
    }

    private Set<Nodeable> getRoots() {
        Set<Nodeable> roots = new HashSet<>();
        for (Nodeable nodeable : graph.vertexSet()) {
            if (graph.inDegreeOf(nodeable) == 0) {
                roots.add(nodeable);
            }
        }
        return roots;
    }

    /**
     * Gets the direct parents of a nodeable in the original {@link #graph}.
     *
     * @param nodeable the nodeable to get the parent of
     * @return set of nodables that are the parents of nodeable
     */
    private Set<Nodeable> getOriginalParents(Nodeable nodeable) {
        return graph.incomingEdgesOf(nodeable).stream().map(graph::getEdgeSource).collect(Collectors.toSet());
    }

    /**
     * Gets the direct children of a nodeable in the original {@link #graph}.
     *
     * @param nodeable the nodeable to get the children of
     * @return set of nodables that are the children of nodeable
     */
    private Set<Nodeable> getOriginalChildren(Nodeable nodeable) {
        return graph.outgoingEdgesOf(nodeable).stream().map(graph::getEdgeTarget).collect(Collectors.toSet());
    }

    /**
     * A holder of intermediate information in the schedule algorithm.
     * <p>
     * Holds all nodeable that are still todos and still need to be scheduled.
     * Also holds the last node that was handled.
     */
    private class Intermediate {
        private final Set<Nodeable> todos;
        private final Node last;

        /**
         * Default constructor.
         *
         * @param todos
         * @param last
         */
        protected Intermediate(Set<Nodeable> todos, Node last) {
            this.last = last;
            this.todos = todos;
        }

        protected Set<Nodeable> getTodos() {
            return todos;
        }

        protected Node getLast() {
            return last;
        }
    }
}
