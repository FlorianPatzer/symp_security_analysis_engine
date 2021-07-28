package de.fraunhofer.iosb.svs.sae.workflowmanager.model;

import de.fraunhofer.iosb.svs.sae.workflowmanager.bpmn.Nodeable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class JenaCombination implements Nodeable, Combination{
    private final List<JenaRule> rules;

    @Override
    public List<JenaRule> getRules() {
        return this.rules;
    }
}
