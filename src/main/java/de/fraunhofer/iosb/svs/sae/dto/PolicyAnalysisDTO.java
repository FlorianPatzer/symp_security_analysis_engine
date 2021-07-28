package de.fraunhofer.iosb.svs.sae.dto;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class PolicyAnalysisDTO {

    @NonNull
    private final String localName;
    @NonNull
    private final String uri;
    @NonNull
    private final String modelLink;
    private final String description;
    private final LocalDateTime lastChanged;
}
