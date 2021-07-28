package de.fraunhofer.iosb.svs.sae.workflowmanager.model;

import de.fraunhofer.iosb.svs.sae.workflowmanager.bpmn.Nodeable;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class SwrlCombination implements Nodeable, Combination {
    private final List<SwrlRule> rules;

    @Override
    public List<SwrlRule> getRules() {
        return this.rules;
    }
}
