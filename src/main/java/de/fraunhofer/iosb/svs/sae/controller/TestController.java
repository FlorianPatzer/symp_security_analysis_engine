package de.fraunhofer.iosb.svs.sae.controller;

import de.fraunhofer.iosb.svs.sae.configuration.RestApiV1Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestApiV1Controller
public class TestController {

    @GetMapping(path = "/test/{id}", produces = "application/json")
    public String test(@PathVariable("id") Long id) {
        return id.toString();
    }
    
    @GetMapping(path = "/demo/{id}", produces = "application/json")
    public String demo(@PathVariable("id") Long id) {
        return id.toString();
    }
}
