package de.fraunhofer.iosb.svs.sae.restclient.sme;

import de.fraunhofer.iosb.svs.sae.exceptions.ResourceNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class SmeClientService {

    private static final Logger log = LoggerFactory.getLogger(SmeClientService.class);

    private final WebClient webClient;

    public SmeClientService(@Value("${sme.endpoint}") String spcEndpoint) {
        this.webClient = WebClient.builder()
                .baseUrl(spcEndpoint + "/api")
                .build();
    }

    /**
     * Pull a target system from the static processing controller.
     * <p>
     * Blocking. Throws {@link ResourceNotFoundException} if spc returns 404 not found.
     *
     * @param targetSystemId
     * @return
     */
    public TargetSystem getTargetSystem(long targetSystemId) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/targetsystem/{id}")
                            .build(targetSystemId))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(TargetSystem.class)
                    .block();
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                // targetSystem does not exist
                log.warn("Static Processing Controller has no Target System with id {}", targetSystemId);
                throw new ResourceNotFoundException("Static Processing Controller has no Target System with id " + targetSystemId);
            } else {
                // pass other errors
                throw ex;
            }
        }
    }
}
