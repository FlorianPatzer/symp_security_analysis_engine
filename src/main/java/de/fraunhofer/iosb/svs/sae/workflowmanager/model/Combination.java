package de.fraunhofer.iosb.svs.sae.workflowmanager.model;

import de.fraunhofer.iosb.svs.sae.workflowmanager.bpmn.Nodeable;

import java.util.List;

public interface Combination extends Nodeable {
    List<? extends ProcessingRule> getRules();
}
