package de.fraunhofer.iosb.svs.sae.dto;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;


import java.util.List;

@Getter
@RequiredArgsConstructor
public class AnalysisDTO {
    @NonNull
    private final String uuid;

    private final String name;
    private final String description;

    @NonNull
    private final Long targetSystemId;

    @NonNull
    private final List<String> policyAnalyses;

}
