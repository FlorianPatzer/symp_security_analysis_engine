package de.fraunhofer.iosb.svs.sae.workflowmanager.model;

import static de.fraunhofer.iosb.svs.sae.workflowmanager.model.Policy.POLICY_BASED_ANALYSIS_NS;

public enum Phase {
    KNOWLEDGE_COLLECTION("KnowledgeCollection"),
    KNOWLEDGE_FUSION("KnowledgeFusion"),
    MODEL_CLEANING("ModelCleaning"),
    STATIC_KNOWLEDGE_EXTENSION("StaticKnowledgeExtension"),
    DYNAMIC_KNOWLEDGE_EXTENSION("DynamicKnowledgeExtension"),
    ANALYSIS("Analysis"),
    ANY_PHASE("AnyPhase");

    private final String uri;
    private final String localName;

    Phase(String localName) {
        this.localName = localName;
        this.uri = POLICY_BASED_ANALYSIS_NS + localName;
    }

    public static Phase getPhaseByUri(String uri) {
        for (Phase p : Phase.values()) {
            if (p.getUri().equals(uri)) {
                return p;
            }
        }
        throw new IllegalArgumentException("No phase with Uri: " + uri);
    }

    public String getUri() {
        return uri;
    }

    public String getLocalName() {
        return localName;
    }
}
