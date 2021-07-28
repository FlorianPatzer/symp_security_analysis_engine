package de.fraunhofer.iosb.svs.sae.workflowmanager.bpmn;

import de.fraunhofer.iosb.svs.sae.workflowmanager.bpmn.nodes.Connector;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Wraps a {@link Nodeable} and adds a id to it. The id is unique for every Node.
 */
public class Node {

    private static AtomicLong idCounter = new AtomicLong();
    private final Nodeable nodeable;
    private String id;

    /**
     * Takes a {@link Nodeable} that should be wrapped and adds a derived {@link #id} using the {@link #idCounter}.
     *
     * @param nodeable the {@link Nodeable} to be wrapped
     */
    public Node(Nodeable nodeable) {
        this.nodeable = nodeable;
        if (nodeable instanceof Connector) {
            Connector connector = (Connector) nodeable;
            if (connector == Connector.REMERGE) {
                this.id = "OntRemerge_" + createID();
            } else {
                this.id = "ParallelGateway_" + createID();
            }
        } else {
            this.id = "ServiceTask_" + createID();
        }
    }

    /**
     * Gets the current value of the {@link #idCounter} and increments it.
     *
     * @return a unique long value
     */
    private static long createID() {
        return idCounter.getAndIncrement();
    }

    /**
     * Gets the wrapped nodeable.
     *
     * @return the wrapped nodeable
     */
    public Nodeable getNodeable() {
        return nodeable;
    }

    /**
     * Checks if the wrapped nodeable is the specified {@link Connector}.
     *
     * @param connector the {@link Connector} to check
     * @return if the wrapped nodeable is the {@link Connector}
     */
    public boolean is(Connector connector) {
        return this.nodeable == connector;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Node{" +
                "id='" + id + '\'' +
                ", nodeable=" + nodeable +
                '}';
    }
}
