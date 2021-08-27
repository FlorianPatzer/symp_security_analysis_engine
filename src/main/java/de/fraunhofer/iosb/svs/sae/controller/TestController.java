package de.fraunhofer.iosb.svs.sae.controller;

import de.fraunhofer.iosb.svs.sae.configuration.RestApiV1Controller;
import de.fraunhofer.iosb.svs.sae.security.RestTemplateWithReloadableTruststore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestTemplate;

@RestApiV1Controller
public class TestController {
    @Autowired
    private RestTemplateWithReloadableTruststore restTemplateCreator;

    @GetMapping(path = "/test/{id}", produces = "application/json")
    public String test(@PathVariable("id") Long id) {
        return id.toString();
    }

    @GetMapping(path = "/test/client-backend-proxy", produces = "application/json")
    public String backendTest() throws Exception {
        RestTemplate restTemplate = restTemplateCreator.create();
        return restTemplate.getForEntity("https://localhost:3000/backend", String.class).getBody();
    }
}
