package de.fraunhofer.iosb.svs.sae;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import de.fraunhofer.iosb.svs.sae.security.RestTemplateWithoutCAValidation;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InitialTests {
    @Value("${sme.endpoint}")
    String smeEndpoint;

    @Value("${ah.endpoint}")
    String ahEndpoint;

    @Value("${http.protocol}")
    String httpProtocol;
    
    @Value("${tests.ah.policyimplementation}")
    String testPolicyImplementation;
    
    @Value("${tests.sme.targetsystemId}")
    String testTargetSystemId;

    @LocalServerPort
    private int port;

    private static RestTemplate restTemplate;

    @BeforeAll
    static void setUp() throws Exception {
        restTemplate = RestTemplateWithoutCAValidation.create();
    }

    // 1. Test if all components are running
    @Test
    @Order(1)
    void checkServicesAvailablability() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
 
        HttpEntity<?> entity = new HttpEntity<Object>(headers);

        ResponseEntity<String> spcResponse = restTemplate.exchange(smeEndpoint + "/api/", HttpMethod.GET,
                entity, String.class);
        
        ResponseEntity<String> ahResponse = restTemplate.exchange(ahEndpoint + "/api/v1/", HttpMethod.GET,
                entity, String.class);

        assertThat(spcResponse.getStatusCode(), is(HttpStatus.OK));
        assertThat(ahResponse.getStatusCode(), is(HttpStatus.OK));
    }

    // 2. Test if AH contains policy implementation
    @Test
    @Order(2)
    void checkPolicyImplementationAvailability() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
 
        HttpEntity<?> entity = new HttpEntity<Object>(headers);

        ResponseEntity<String> ahResponse = restTemplate.exchange(ahEndpoint + "/api/v1/policyimplementation/" + testPolicyImplementation, HttpMethod.GET,
                entity, String.class);

        assertThat(ahResponse.getStatusCode(), is(HttpStatus.OK));
    }
    
    // 3. Test if SPC contains target system
    @Test
    @Order(3)
    void checkTargetSystemAvailability() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
 
        HttpEntity<?> entity = new HttpEntity<Object>(headers);

        ResponseEntity<String> spcResponse = restTemplate.exchange(smeEndpoint + "/api/targetsystem/" + testTargetSystemId, HttpMethod.GET,
                entity, String.class);

        assertThat(spcResponse.getStatusCode(), is(HttpStatus.OK));
    }
}
